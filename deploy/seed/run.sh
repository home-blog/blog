#!/usr/bin/env bash
# 실습 서버에서 실행: 예시 데이터를 배포 DB에 넣거나(add) 지운다(remove).
# 사용: bash ~/geulgyeol/deploy/seed/run.sh add      (넣기, 한 번만 된다)
#       bash ~/geulgyeol/deploy/seed/run.sh remove   (지우기)
# DB 접속 정보는 배포 때 만들어진 ~/geulgyeol/.env.deploy에서 읽는다. 비밀번호는 화면에 나오지 않는다.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
root="$here/../.."
case "${1:-}" in
  add) file="$here/demo-content.sql" ;;
  remove) file="$here/demo-remove.sql" ;;
  *) echo "사용: bash $0 add|remove" >&2; exit 1 ;;
esac
[ -f "$root/.env.deploy" ] || { echo "$root/.env.deploy가 없습니다. 먼저 배포를 해 주세요." >&2; exit 1; }
set -a
# shellcheck source=/dev/null
. "$root/.env.deploy"
set +a
# 서버 컨테이너와 같은 네트워크에서 DB에 붙는다 (서버가 DB에 닿는 길과 같다)
docker run --rm -i --network "${DEPLOY_NAME}_default" -e PGPASSWORD="$DB_PASSWORD" postgres:18-alpine \
  psql -q "host=$DB_ADDRESS port=${DB_PORT:-5432} dbname=$DB_NAME user=$DB_USERNAME" -v schema="$DB_SCHEMA" < "$file"
