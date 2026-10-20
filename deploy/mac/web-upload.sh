#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 服务器信息不入库：命令行环境变量优先；否则读本地 deploy.env（已 gitignore，模板见 deploy.env.example）
if [[ -z "${FRONTEND_SERVER_HOST:-}" && -f "$SCRIPT_DIR/deploy.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$SCRIPT_DIR/deploy.env"
  set +a
fi
: "${FRONTEND_SERVER_HOST:?未配置目标服务器：export FRONTEND_SERVER_HOST=...，或复制 deploy.env.example 为 deploy.env 后填写}"
SERVER_HOST="$FRONTEND_SERVER_HOST"
SERVER_USER="${FRONTEND_SERVER_USER:-root}"
REMOTE_PASSWORD="${REMOTE_PASSWORD:-}"
LOCAL_DIST="$PROJECT_ROOT/blog-web/dist"
REMOTE_DIST_DIR="${REMOTE_DIST_DIR:-/mydata/nginx/html/blog/web}"

for command_name in npm ssh scp; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "缺少命令：$command_name" >&2
    exit 127
  }
done

cd "$PROJECT_ROOT/blog-web"
npm run build
[[ -d "$LOCAL_DIST" ]] || { echo "前端构建产物不存在：$LOCAL_DIST" >&2; exit 1; }

# SSH 通道：公钥免密优先，未配置公钥时才要求密码（sshpass）
if [[ -z "$REMOTE_PASSWORD" ]] \
   && ssh -o BatchMode=yes -o ConnectTimeout=5 "$SERVER_USER@$SERVER_HOST" true 2>/dev/null; then
  SSH_COMMAND=(ssh -o StrictHostKeyChecking=no)
  SCP_COMMAND=(scp -O)
else
  if [[ -z "$REMOTE_PASSWORD" ]]; then
    read -r -s -p "公钥未配置，请输入 $SERVER_USER@$SERVER_HOST 的密码：" REMOTE_PASSWORD
    echo >&2
  fi
  command -v sshpass >/dev/null 2>&1 || {
    echo "缺少命令：sshpass（brew install sshpass），或先配置 SSH 公钥免密" >&2
    exit 127
  }
  export SSHPASS="$REMOTE_PASSWORD"
  SSH_COMMAND=(sshpass -e ssh -o StrictHostKeyChecking=no)
  SCP_COMMAND=(sshpass -e scp -O)
fi

"${SSH_COMMAND[@]}" "$SERVER_USER@$SERVER_HOST" "mkdir -p '$REMOTE_DIST_DIR'"
"${SSH_COMMAND[@]}" "$SERVER_USER@$SERVER_HOST" "rm -rf '$REMOTE_DIST_DIR/index.html' '$REMOTE_DIST_DIR/favicon.ico' '$REMOTE_DIST_DIR/static'"
"${SCP_COMMAND[@]}" -r -p -q "$LOCAL_DIST"/* "$SERVER_USER@$SERVER_HOST:$REMOTE_DIST_DIR/"
