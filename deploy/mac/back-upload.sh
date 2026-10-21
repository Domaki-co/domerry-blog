#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 服务器信息不入库：命令行环境变量优先；否则读本地 deploy.env（已 gitignore，模板见 deploy.env.example）
if [[ -z "${BACKEND_SERVER_HOST:-}" && -f "$SCRIPT_DIR/deploy.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$SCRIPT_DIR/deploy.env"
  set +a
fi
: "${BACKEND_SERVER_HOST:?未配置目标服务器：export BACKEND_SERVER_HOST=...，或复制 deploy.env.example 为 deploy.env 后填写}"
SERVER_HOST="$BACKEND_SERVER_HOST"
SERVER_USER="${BACKEND_SERVER_USER:-blog}"
REMOTE_PASSWORD="${REMOTE_PASSWORD:-}"
JAR_NAME="domerry-blog.jar"
LOCAL_JAR="$PROJECT_ROOT/blog/domerry-server/target/$JAR_NAME"
REMOTE_JAR_NAME="domerry-blog.jar"
REMOTE_JAR_DIR="${REMOTE_JAR_DIR:-}"
REMOTE_DEPLOY_SCRIPT_DIR="${REMOTE_DEPLOY_SCRIPT_DIR:-$REMOTE_JAR_DIR}"

# 项目是 Java 8 + 旧版 Lombok，JDK 16+ 编译会报 IllegalAccessError，固定用 JDK 8 打包
BUILD_JAVA_HOME="${BUILD_JAVA_HOME:-$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)}"
if [[ -n "$BUILD_JAVA_HOME" ]]; then
  export JAVA_HOME="$BUILD_JAVA_HOME"
fi

for command_name in mvn ssh scp; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "缺少命令：$command_name" >&2
    exit 127
  }
done

cd "$PROJECT_ROOT/blog"
mvn clean package
[[ -f "$LOCAL_JAR" ]] || { echo "打包产物不存在：$LOCAL_JAR" >&2; exit 1; }

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

# 目录必须已存在，脚本不创建（mkdir 会导致失败）
"${SCP_COMMAND[@]}" -q "$LOCAL_JAR" "$SERVER_USER@$SERVER_HOST:$REMOTE_JAR_DIR/$REMOTE_JAR_NAME"
"${SSH_COMMAND[@]}" "$SERVER_USER@$SERVER_HOST" \
  "cd '$REMOTE_DEPLOY_SCRIPT_DIR' && bash docker_build.sh"
