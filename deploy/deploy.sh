#!/usr/bin/env bash
# 실습 서버에서 실행: 최신 코드를 받아 컨테이너를 새로 만들고 켠다. 데이터(DB, 이미지)는 그대로 남는다.
# 사용: 저장소 폴더에서 bash deploy/deploy.sh
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f .env.deploy ]; then
  echo ".env.deploy가 없습니다. cp .env.deploy.example .env.deploy 뒤 값을 채우세요." >&2
  exit 1
fi
git pull --ff-only
docker compose -f docker-compose.deploy.yml --env-file .env.deploy up -d --build --wait
docker compose -f docker-compose.deploy.yml --env-file .env.deploy ps
port="$(grep -E '^WEB_PORT=' .env.deploy | cut -d= -f2)"
echo "켜졌습니다. 서버 안에서 확인: curl -s http://127.0.0.1:${port}/api/auth/csrf"
