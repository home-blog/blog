#!/usr/bin/env bash
# 실습 서버에서 실행: 내 도메인·포트로 Nginx 설정 파일(~/nginx/<도메인>.conf)을 만든다.
# 사용: bash ~/<DEPLOY_NAME>/deploy/make-nginx-conf.sh <도메인> [a 포트] [b 포트]
#       포트를 적지 않으면 .env.deploy의 WEB_PORT·WEB_PORT_B를 쓴다 (docker compose가 여는 포트와 같아야 한다)
# 이미 있는 파일(강사님이 HTTPS를 덧붙였을 수 있다)은 덮어쓰지 않는다. 고칠 때는 deploy/README.md의 "학원 Nginx 고치기"대로 한 줄씩
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
domain="${1:?도메인을 적어 주세요. 예: bash deploy/make-nginx-conf.sh myblog.example.net}"
port="${2:-}" port_b="${3:-}"
env_port() { grep -E "^$1=" "$here/../.env.deploy" 2>/dev/null | tail -n 1 | cut -d= -f2 | tr -d '[:space:]' || true; }
[ -n "$port" ] || port="$(env_port WEB_PORT)"
[ -n "$port_b" ] || port_b="$(env_port WEB_PORT_B)"
port="${port:?포트를 적거나 .env.deploy에 WEB_PORT를 적어 주세요}"
port_b="${port_b:?b 벌 포트를 적거나 .env.deploy에 WEB_PORT_B를 적어 주세요}"
name="${domain%%.*}"   # upstream·로그 모양 이름 (도메인 첫 부분)
name="${name//-/_}"

# 설정 파일에 그대로 들어가는 값이라 모양을 확인한다
[[ "$domain" =~ ^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$ ]] || { echo "도메인 모양이 아닙니다: $domain" >&2; exit 1; }
[[ "$port" =~ ^[0-9]{1,5}$ && "$port_b" =~ ^[0-9]{1,5}$ ]] || { echo "포트는 숫자로 적어 주세요: $port, $port_b" >&2; exit 1; }
[[ "$HOME" =~ ^/[A-Za-z0-9._/-]*$ ]] || { echo "홈 폴더 경로에 쓸 수 없는 글자가 있습니다: $HOME" >&2; exit 1; }

mkdir -p "$HOME/nginx/logs"
out="$HOME/nginx/$domain.conf"
if [ -e "$out" ]; then
  echo "이미 있습니다: $out (덮어쓰지 않습니다. 고칠 곳은 deploy/README.md의 '학원 Nginx 고치기')" >&2
  exit 1
fi
sed -e "s|__DOMAIN__|$domain|g" -e "s|__HOME__|$HOME|g" -e "s|__NAME__|$name|g" \
  -e "s|__PORT_B__|$port_b|g" -e "s|__PORT__|$port|g" \
  "$here/nginx-site.conf.template" > "$out"
echo "만들었습니다: $out (127.0.0.1:$port, 127.0.0.1:$port_b로 번갈아 넘김)"
echo "강사님께 파일 이름 '$domain.conf'를 알려 적용을 요청하세요."
