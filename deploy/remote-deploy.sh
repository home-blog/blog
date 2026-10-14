#!/usr/bin/env bash
# 실습 서버에서 실행: GitHub Actions가 올려 준 이미지로 앱 두 벌(a, b)을 한 벌씩 차례로 바꾼다 (무중단 롤링 배포, specs/011).
# Actions(.github/workflows/deploy.yml)가 SSH로 부른다. 서버에서 직접 부를 때:
#   bash ~/<DEPLOY_NAME>/deploy/remote-deploy.sh           # .env.deploy의 IMAGE_TAG(마지막으로 올린 것)로
#   bash ~/<DEPLOY_NAME>/deploy/remote-deploy.sh <태그>     # 서버에 남아 있는 다른 이미지로 (되돌리기)
#
# 어떻게 안 끊기나: 학원 Nginx가 upstream으로 a(WEB_PORT)·b(WEB_PORT_B)에 번갈아 보내고, 한쪽이 꺼져 있으면 다른 쪽으로
# 다시 보낸다(proxy_next_upstream). 그래서 한 벌을 끄고 바꾸는 동안 다른 벌이 손님을 받는다.
#   1) 한 벌의 화면 입구를 먼저 끈다(포트가 닫혀 Nginx가 다른 벌로 보낸다. 처리 중인 요청은 마치고 꺼진다)
#   2) 그 벌의 서버를 새 이미지로 켜고 건강해질 때까지 기다린 뒤 화면 입구를 켠다
#   3) 그 포트로 직접 확인한다. 실패하면 그 벌만 옛 이미지로 되돌리거나 꺼 두고 멈춘다 (다른 벌은 그대로 손님을 받는다)
#   4) Nginx가 다시 이 벌로 보내기 시작할 때까지(fail_timeout) 기다린 뒤 다음 벌을 바꾼다
# Redis·이미지(MinIO) 데이터는 그대로 남는다.
#
# 이 폴더(~/<DEPLOY_NAME>)에 있는 것
#   docker-compose.deploy.yml, deploy/  : Actions가 저장소에서 복사
#   .env.deploy                         : Actions가 GitHub Secrets로 만든다 (나만 읽기)
#   .generated.env                      : 처음 한 번 이 스크립트가 만든 무작위 비밀 값 (나만 읽기, 지우면 이미지 저장소에 못 들어간다)
#   .slots.env                          : 어느 벌이 어느 이미지인지 (이 스크립트가 쓴다)
#   images.tar.gz                       : Actions가 올린 새 이미지 (불러온 뒤 지운다)
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077

if [ ! -f .env.deploy ]; then
  echo ".env.deploy가 없습니다. GitHub Actions의 배포를 먼저 돌려 주세요." >&2
  exit 1
fi

# GitHub Secrets에 없는 비밀 값은 처음 한 번 여기서 만들고 계속 쓴다
if [ ! -f .generated.env ]; then
  {
    echo "IMAGE_S3_ACCESS_KEY=images-$(openssl rand -hex 6)"
    echo "IMAGE_S3_SECRET_KEY=$(openssl rand -hex 24)"
    echo "VERIFICATION_SECRET=$(openssl rand -hex 32)"
  } > .generated.env
  echo "비밀 값을 새로 만들었습니다: $(pwd)/.generated.env"
fi

# 셋 다 서버에서 만들어지는 파일이라 검사 도구가 따라가지 않게 한다
set -a
# shellcheck source=/dev/null
. ./.env.deploy
# shellcheck source=/dev/null
. ./.generated.env
# shellcheck source=/dev/null
[ -f .slots.env ] && . ./.slots.env
set +a

# 컨테이너 안에서 localhost는 그 컨테이너 자신이라 DB에 닿지 않는다. DB 서버의 실제 주소(이름이나 IP)를 써야 한다
case "$DB_ADDRESS" in
  localhost | 127.* | ::1)
    echo "DB_ADDRESS($DB_ADDRESS)로는 컨테이너에서 DB에 닿지 않습니다. DB 서버의 실제 주소를 Secret에 넣어 주세요." >&2
    exit 1 ;;
esac

target="${1:-${IMAGE_TAG:?IMAGE_TAG가 없습니다}}"
[[ "$target" =~ ^[A-Za-z0-9._-]+$ ]] || { echo "이미지 태그 모양이 아닙니다: $target" >&2; exit 1; }
export WEB_PORT_B="${WEB_PORT_B:-8411}"
# 한 벌을 켠 뒤 다음 벌을 끄기까지 기다리는 시간. 학원 Nginx의 fail_timeout(10s)보다 길어야
# Nginx가 방금 켠 벌을 다시 쓰기 시작한 뒤에 다른 벌을 끈다 (둘 다 쉬는 중으로 보이면 502가 난다)
settle="${SLOT_SETTLE_SECONDS:-15}"
legacy_web="${DEPLOY_NAME}-web-1" legacy_server="${DEPLOY_NAME}-server-1"   # 두 벌 이전(v0.5.1까지)의 컨테이너

compose=(docker compose -f docker-compose.deploy.yml)
# 두 벌 이전 컨테이너(옛 web·server)는 아래에서 직접 끈다. compose가 "남은 컨테이너" 경고를 내지 않게 한다
export COMPOSE_IGNORE_ORPHANS=true
# 아직 한 번도 켜지 않은 벌은 옛 이미지가 없다(처음). compose 파일을 읽으려면 값이 있어야 해서 새 태그로 채워 둔다
first_a="${IMAGE_TAG_A:-}" first_b="${IMAGE_TAG_B:-}"
export IMAGE_TAG_A="${IMAGE_TAG_A:-$target}" IMAGE_TAG_B="${IMAGE_TAG_B:-$target}"
# 되돌리기용으로 남길 "바로 전 이미지": 지금 손님을 받는 이미지 (두 벌 이전이면 옛 서버 컨테이너의 이미지). 같은 태그를 다시 올리면 그대로
running_tag="$first_a"
[ -n "$running_tag" ] || running_tag="$(docker container inspect -f '{{.Config.Image}}' "$legacy_server" 2>/dev/null | sed 's/^.*://' || true)"
previous="${PREVIOUS_TAG:-}"
if [ -n "$running_tag" ] && [ "$running_tag" != "$target" ]; then previous="$running_tag"; fi

save_slots() {
  printf 'IMAGE_TAG_A=%s\nIMAGE_TAG_B=%s\nPREVIOUS_TAG=%s\n' "$IMAGE_TAG_A" "$IMAGE_TAG_B" "$previous" > .slots.env
}
port_of() { if [ "$1" = a ]; then echo "$WEB_PORT"; else echo "$WEB_PORT_B"; fi; }
running() { [ -n "$("${compose[@]}" ps -q --status running "web-$1" 2>/dev/null)" ]; }
exists() { docker container inspect "$1" >/dev/null 2>&1; }

# 그 벌의 포트로 직접 물어본다: API가 200이고 그 벌의 이름(X-Instance)을 대는지, 첫 화면이 200인지
check_slot() {
  local slot="$1" port headers code
  port="$(port_of "$slot")"
  headers="$(curl -s -o /dev/null -D - --max-time 10 "http://127.0.0.1:$port/api/v1/auth/csrf" || true)"
  if ! grep -qE '^HTTP/[0-9.]+ 200' <<<"$headers"; then
    echo "확인 실패: $slot 벌(127.0.0.1:$port) API가 200으로 답하지 않습니다" >&2; return 1
  fi
  if ! grep -qiE "^x-instance: $slot"$'\r'"?$" <<<"$headers"; then
    echo "확인 실패: 127.0.0.1:$port 가 $slot 벌의 서버로 이어지지 않습니다" >&2; return 1
  fi
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "http://127.0.0.1:$port/" || true)"
  [ "$code" = 200 ] || { echo "확인 실패: $slot 벌 첫 화면이 ${code}로 답했습니다" >&2; return 1; }
}

if [ -f images.tar.gz ]; then
  echo "새 이미지 불러오기 (${IMAGE_TAG:-})"
  gunzip -c images.tar.gz | docker load
  rm -f images.tar.gz
fi
for repo in server web; do
  docker image inspect "${DEPLOY_NAME}-$repo:$target" >/dev/null 2>&1 \
    || { echo "이미지 ${DEPLOY_NAME}-$repo:$target 가 서버에 없습니다" >&2; exit 1; }
done

# 두 벌이 같이 쓰는 것 (바뀐 게 없으면 그대로 둔다)
"${compose[@]}" up -d --no-build --wait redis minio

# 차례: 두 벌 이전 구성이면 비어 있는 b부터 (a의 포트를 옛 화면 컨테이너가 쓰고 있다). 꺼져 있는 벌이 있으면 그 벌부터
if exists "$legacy_web" || exists "$legacy_server"; then order=(b a)
elif ! running a; then order=(a b)
elif ! running b; then order=(b a)
else order=(a b)
fi

for i in "${!order[@]}"; do
  slot="${order[$i]}"
  var="IMAGE_TAG_${slot^^}" first="first_$slot"
  old="${!first}"   # 이 벌이 지금 쓰는 이미지 (처음이면 비어 있다)
  echo "── $slot 벌: ${old:-처음} → ${target} (127.0.0.1:$(port_of "$slot"))"

  if [ "$slot" = a ] && { exists "$legacy_web" || exists "$legacy_server"; }; then
    echo "두 벌 이전의 컨테이너를 끕니다 (손님은 b 벌이 받는다)"
    docker stop -t 40 "$legacy_web" >/dev/null 2>&1 || true
    docker stop -t 40 "$legacy_server" >/dev/null 2>&1 || true
    docker rm "$legacy_web" "$legacy_server" >/dev/null 2>&1 || true
  fi

  # 1) 화면 입구부터 끈다: 포트가 닫히면 학원 Nginx가 다른 벌로 보낸다
  if running "$slot"; then "${compose[@]}" stop "web-$slot"; fi

  # 2) 새 이미지로 켠다
  export "$var=$target"
  if ! "${compose[@]}" up -d --no-build --no-deps --wait --wait-timeout 300 "server-$slot" "web-$slot" || ! check_slot "$slot"; then
    "${compose[@]}" logs --tail 80 "server-$slot" >&2 || true
    if [ -n "$old" ] && [ "$old" != "$target" ] && docker image inspect "${DEPLOY_NAME}-server:$old" >/dev/null 2>&1; then
      echo "$slot 벌을 옛 이미지($old)로 되돌립니다" >&2
      export "$var=$old"
      "${compose[@]}" up -d --no-build --no-deps --wait --wait-timeout 300 "server-$slot" "web-$slot" || true
    else
      echo "$slot 벌을 꺼 둡니다 (고장 난 벌로 손님이 가지 않게)" >&2
      "${compose[@]}" stop "web-$slot" "server-$slot" || true
      export "$var=$old"   # 처음 켜다 실패했으면 기록하지 않는다 (다음 배포 때 이 벌부터 다시 켠다)
    fi
    save_slots
    echo "배포 실패: $slot 벌에서 멈췄습니다. 다른 벌은 그대로 손님을 받습니다" >&2
    exit 1
  fi
  save_slots
  echo "$slot 벌 켜짐"

  # 3) 다음 벌을 끄기 전에 학원 Nginx가 이 벌을 다시 쓰기 시작할 때까지 기다린다
  if [ "$i" -lt $((${#order[@]} - 1)) ]; then
    echo "${settle}초 기다린 뒤 다음 벌을 바꿉니다"
    sleep "$settle"
  fi
done

"${compose[@]}" ps
echo "켜졌습니다: a 127.0.0.1:${WEB_PORT}, b 127.0.0.1:${WEB_PORT_B} (${target})"

# 지금 두 벌이 쓰는 이미지와 바로 전 이미지(되돌리기용)만 남기고 내 옛 이미지를 지운다 (여럿이 쓰는 Docker라 다른 사람 이미지는 건드리지 않는다)
for repo in "${DEPLOY_NAME}-server" "${DEPLOY_NAME}-web"; do
  docker image ls "$repo" --format '{{.Repository}}:{{.Tag}}' \
    | grep -vxF -e "${repo}:${IMAGE_TAG_A}" -e "${repo}:${IMAGE_TAG_B}" -e "${repo}:${previous}" \
    | xargs -r docker image rm >/dev/null 2>&1 || true
done
