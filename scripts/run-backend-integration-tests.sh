#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# 复用开发环境真实数据库与 Redis 凭据，确保测试链路与本机容器一致。
source "${PROJECT_ROOT}/backend/.env.cloud-dev.local"

cd "${PROJECT_ROOT}/backend"
mvn test "$@"
