#!/usr/bin/env bash
# 실습 서버에서 실행: GitHub Actions가 올려 준 이미지로 컨테이너를 새로 켠다. Redis·이미지(MinIO) 데이터는 그대로 남는다.
# Actions(.github/workflows/deploy.yml)가 SSH로 부른다. 서버에서 직접 다시 켤 때: bash ~/<DEPLOY_NAME>/deploy/remote-deploy.sh
#
# 이 폴더(~/<DEPLOY_NAME>)에 있는 것
#   docker-compose.deploy.yml, deploy/  : Actions가 저장소에서 복사
#   .env.deploy                         : Actions가 GitHub Secrets로 만든다 (나만 읽기)
#   .generated.env                      : 처음 한 번 이 스크립트가 만든 무작위 비밀 값 (나만 읽기, 지우면 이미지 저장소에 못 들어간다)
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

# 둘 다 서버에서 만들어지는 파일이라 검사 도구가 따라가지 않게 한다
set -a
# shellcheck source=/dev/null
. ./.env.deploy
# shellcheck source=/dev/null
. ./.generated.env
set +a

# DB가 이 서버 자신(localhost)이면 컨테이너 안에서는 host.docker.internal로 닿는다
case "$DB_ADDRESS" in
  localhost | 127.0.0.1 | ::1) DB_ADDRESS=host.docker.internal ;;
esac

compose=(docker compose -f docker-compose.deploy.yml)

if [ -f images.tar.gz ]; then
  echo "새 이미지 불러오기 (${IMAGE_TAG})"
  gunzip -c images.tar.gz | docker load
  rm -f images.tar.gz
fi

"${compose[@]}" up -d --no-build --wait --remove-orphans
"${compose[@]}" ps

# 바깥 입구(화면 nginx → 서버)까지 답하는지 확인
code="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${WEB_PORT}/api/auth/csrf" || true)"
if [ "$code" != "200" ]; then
  echo "확인 실패: http://127.0.0.1:${WEB_PORT}/api/auth/csrf 가 ${code}로 답했습니다. 서버 로그: ${compose[*]} logs server" >&2
  exit 1
fi
echo "켜졌습니다: 127.0.0.1:${WEB_PORT} (${IMAGE_TAG})"

# 이번에 쓰지 않는 내 옛 이미지만 지운다 (여럿이 쓰는 Docker라 다른 사람 이미지는 건드리지 않는다)
for repo in "${DEPLOY_NAME}-server" "${DEPLOY_NAME}-web"; do
  docker image ls "$repo" --format '{{.Repository}}:{{.Tag}}' | grep -vxF "${repo}:${IMAGE_TAG}" | xargs -r docker image rm >/dev/null || true
done
