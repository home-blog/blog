#!/usr/bin/env bash
# 실습 서버에서 실행: 내 도메인·포트로 Nginx 설정 파일(~/nginx/<도메인>.conf)을 만든다.
# 사용: bash deploy/make-nginx-conf.sh <도메인> [포트, 기본 8410]
set -euo pipefail
domain="${1:?도메인을 적어 주세요. 예: bash deploy/make-nginx-conf.sh myblog.example.net 8410}"
port="${2:-8410}"
here="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$HOME/nginx"
out="$HOME/nginx/$domain.conf"
sed -e "s|__DOMAIN__|$domain|g" -e "s|__HOME__|$HOME|g" -e "s|__PORT__|$port|g" \
  "$here/nginx-site.conf.template" > "$out"
echo "만들었습니다: $out"
echo "강사님께 파일 이름 '$domain.conf'를 알려 적용을 요청하세요."
