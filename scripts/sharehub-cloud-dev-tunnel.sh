#!/usr/bin/env bash

set -euo pipefail

SERVER_HOST="${SERVER_HOST:-159.65.132.59}"
SERVER_USER="${SERVER_USER:-root}"
SERVER_PORT="${SERVER_PORT:-22}"
LOCAL_POSTGRES_PORT="${LOCAL_POSTGRES_PORT:-55432}"
REMOTE_POSTGRES_PORT="${REMOTE_POSTGRES_PORT:-55432}"
LOCAL_REDIS_PORT="${LOCAL_REDIS_PORT:-56379}"
REMOTE_REDIS_PORT="${REMOTE_REDIS_PORT:-56379}"

if ! command -v sshpass >/dev/null 2>&1; then
  echo "缺少 sshpass，请先安装：brew install hudochenkov/sshpass/sshpass" >&2
  exit 1
fi

if [[ -z "${SSH_PASSWORD:-}" ]]; then
  echo "请先设置 SSH_PASSWORD 环境变量。" >&2
  exit 1
fi

echo "建立 SSH 隧道：localhost:${LOCAL_POSTGRES_PORT} -> ${SERVER_HOST}:127.0.0.1:${REMOTE_POSTGRES_PORT}"
echo "建立 SSH 隧道：localhost:${LOCAL_REDIS_PORT} -> ${SERVER_HOST}:127.0.0.1:${REMOTE_REDIS_PORT}"

exec sshpass -p "${SSH_PASSWORD}" ssh \
  -o PreferredAuthentications=password \
  -o PubkeyAuthentication=no \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=3 \
  -o StrictHostKeyChecking=no \
  -N \
  -L "${LOCAL_POSTGRES_PORT}:127.0.0.1:${REMOTE_POSTGRES_PORT}" \
  -L "${LOCAL_REDIS_PORT}:127.0.0.1:${REMOTE_REDIS_PORT}" \
  -p "${SERVER_PORT}" \
  "${SERVER_USER}@${SERVER_HOST}"
