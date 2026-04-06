#!/usr/bin/env bash
set -euo pipefail

# 检查前端是否存在直接硬编码颜色（允许 token 文件）
violations=$(rg -n "#[0-9a-fA-F]{3,8}" frontend/src --glob '!**/styles/tokens.css' --glob '!**/contracts/design-tokens.json' || true)
if [[ -n "$violations" ]]; then
  echo "发现硬编码色值（不符合 token 规范）："
  echo "$violations"
  exit 1
fi

echo "Token 校验通过：未发现硬编码色值"
