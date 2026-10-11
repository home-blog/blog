#!/usr/bin/env bash
# 실습 서버에서 실행: 내 도메인·포트로 Nginx 설정 파일(~/nginx/<도메인>.conf)을 만든다.
# 사용: bash deploy/make-nginx-conf.sh <도메인> [포트]
#       포트를 적지 않으면 .env.deploy의 WEB_PORT를 쓴다 (docker compose가 여는 포트와 같아야 한다)
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
domain="${1:?도메인을 적어 주세요. 예: bash deploy/make-nginx-conf.sh myblog.example.net}"
port="${2:-}"
if [ -z "$port" ] && [ -f "$here/../.env.deploy" ]; then
  port="$(grep -E '^WEB_PORT=' "$here/../.env.deploy" | tail -n 1 | cut -d= -f2 | tr -d '[:space:]')"
fi
port="${port:?포트를 적거나 .env.deploy에 WEB_PORT를 적어 주세요}"

# 설정 파일에 그대로 들어가는 값이라 모양을 확인한다
[[ "$domain" =~ ^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$ ]] || { echo "도메인 모양이 아닙니다: $domain" >&2; exit 1; }
[[ "$port" =~ ^[0-9]{1,5}$ ]] || { echo "포트는 숫자로 적어 주세요: $port" >&2; exit 1; }
[[ "$HOME" =~ ^/[A-Za-z0-9._/-]*$ ]] || { echo "홈 폴더 경로에 쓸 수 없는 글자가 있습니다: $HOME" >&2; exit 1; }

mkdir -p "$HOME/nginx/logs"
out="$HOME/nginx/$domain.conf"
sed -e "s|__DOMAIN__|$domain|g" -e "s|__HOME__|$HOME|g" -e "s|__PORT__|$port|g" \
  "$here/nginx-site.conf.template" > "$out"
echo "만들었습니다: $out (127.0.0.1:$port로 넘김)"
echo "강사님께 파일 이름 '$domain.conf'를 알려 적용을 요청하세요."
