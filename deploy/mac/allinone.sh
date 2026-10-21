#!/usr/bin/env bash
set -euo pipefail

# 三个上传脚本（admin-upload.sh / web-upload.sh / back-upload.sh）的整合版。
# 用法：
#   ./allinone.sh                # 等价于 all：web + admin + back
#   ./allinone.sh admin          # 只发管理后台
#   ./allinone.sh web            # 只发门户
#   ./allinone.sh back           # 只发后端（构建 jar 并触发远程 docker_build.sh）
#   ./allinone.sh web admin      # 任意组合，按传入顺序执行
#   ./allinone.sh all --dry-run  # 只打印将执行的命令，不实际构建/上传
# 服务器信息不入库：命令行环境变量优先；否则读本地 deploy.env（已 gitignore，模板见 deploy.env.example）

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

DRY_RUN=false
usage() {
  cat <<'EOF'
用法: ./allinone.sh [admin|web|back|all ...] [--dry-run]
  admin  构建 blog-admin 并上传到前端服务器
  web    构建 blog-web 并上传到前端服务器
  back   打包后端 jar 上传到后端服务器并执行远程 docker_build.sh
  all    以上三个全部执行（缺省目标）
  --dry-run 只打印将执行的命令
EOF
}

TARGETS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    admin|web|back|all) TARGETS+=("$1"); shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    -h|--help|help) usage; exit 0 ;;
    *) echo "未知参数：$1" >&2; usage >&2; exit 64 ;;
  esac
done
if [[ ${#TARGETS[@]} -eq 0 ]]; then TARGETS=(all); fi

# all 展开为三个目标，并按传入顺序去重
PLAN=()
SEEN=" "
for target in "${TARGETS[@]}"; do
  if [[ "$target" == all ]]; then items=(web admin back); else items=("$target"); fi
  for item in "${items[@]}"; do
    if [[ "$SEEN" != *" $item "* ]]; then
      SEEN+="$item "
      PLAN+=("$item")
    fi
  done
done

# 服务器信息不入库：命令行环境变量优先；否则读本地 deploy.env
if [[ -f "$SCRIPT_DIR/deploy.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$SCRIPT_DIR/deploy.env"
  set +a
fi
REMOTE_PASSWORD="${REMOTE_PASSWORD:-}"

ADMIN_REMOTE_DIR="${ADMIN_REMOTE_DIR:-/mydata/nginx/html/blog/admin}"
WEB_REMOTE_DIR="${WEB_REMOTE_DIR:-/mydata/nginx/html/blog/web}"
REMOTE_JAR_DIR="${REMOTE_JAR_DIR:-/opt/domerry/blog}"
REMOTE_DEPLOY_SCRIPT_DIR="${REMOTE_DEPLOY_SCRIPT_DIR:-$REMOTE_JAR_DIR}"

step() { echo "==> $*"; }
die() { echo "错误：$*" >&2; exit 1; }

# ---- SSH 通道：公钥免密优先，未配置公钥时才要密码（sshpass）----
# 只在切换主机时重新探测，多目标共用一次密码输入
CHANNEL_ID=""
CHANNEL_KIND=""
ensure_channel() {
  local id="$1@$2"
  if [[ "$id" == "$CHANNEL_ID" ]]; then return 0; fi
  if [[ "$DRY_RUN" == true ]]; then
    echo "[dry-run] 跳过 SSH 探测 ${id}（假定为公钥免密通道）"
    CHANNEL_ID="$id"; CHANNEL_KIND="key"
    return 0
  fi
  if [[ -z "$REMOTE_PASSWORD" ]] \
     && ssh -o BatchMode=yes -o ConnectTimeout=5 "$id" true 2>/dev/null; then
    CHANNEL_ID="$id"; CHANNEL_KIND="key"
  else
    if [[ -z "$REMOTE_PASSWORD" ]]; then
      read -r -s -p "公钥未配置，请输入 $id 的密码：" REMOTE_PASSWORD
      echo >&2
    fi
    command -v sshpass >/dev/null 2>&1 || {
      echo "缺少命令：sshpass（brew install sshpass），或先配置 SSH 公钥免密" >&2
      exit 127
    }
    export SSHPASS="$REMOTE_PASSWORD"
    CHANNEL_ID="$id"; CHANNEL_KIND="password"
  fi
}
# $1 = user@host，其余为远程命令；$2 起作为 ssh 的远程命令整体
run_ssh_on() {
  local dest="$1"; shift
  if [[ "$DRY_RUN" == true ]]; then
    echo "[dry-run] ssh -o StrictHostKeyChecking=no $dest $*"
    return 0
  fi
  if [[ "$CHANNEL_KIND" == password ]]; then
    sshpass -e ssh -o StrictHostKeyChecking=no "$dest" "$@"
  else
    ssh -o StrictHostKeyChecking=no "$dest" "$@"
  fi
}
# $1 = user@host:path（目标），其余为源文件参数（scp 源在前、目标在后）
run_scp_to() {
  local dest="$1"; shift
  if [[ "$DRY_RUN" == true ]]; then
    echo "[dry-run] scp -O $* $dest"
    return 0
  fi
  if [[ "$CHANNEL_KIND" == password ]]; then
    sshpass -e scp -O "$@" "$dest"
  else
    scp -O "$@" "$dest"
  fi
}

# ---- 部署目标 ----
# 前端（admin / web）：构建 → 清理远端旧产物 → 上传 dist
deploy_front() {
  local target="$1" app_dir remote_dir label
  if [[ "$target" == admin ]]; then
    app_dir="blog-admin"; remote_dir="$ADMIN_REMOTE_DIR"; label="管理后台"
  else
    app_dir="blog-web"; remote_dir="$WEB_REMOTE_DIR"; label="门户"
  fi
  local server_host="$FRONTEND_SERVER_HOST"
  local server_user="${FRONTEND_SERVER_USER:-root}"
  local dist="$PROJECT_ROOT/$app_dir/dist"

  step "[$label] 构建产物（npm run build）"
  for command_name in npm ssh scp; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "缺少命令：$command_name" >&2; exit 127; }
  done
  (cd "$PROJECT_ROOT/$app_dir" && if [[ "$DRY_RUN" == true ]]; then echo "[dry-run] npm run build"; else npm run build; fi)
  if [[ "$DRY_RUN" != true ]]; then
    [[ -d "$dist" ]] || die "前端构建产物不存在：$dist"
  fi

  ensure_channel "$server_user" "$server_host"
  step "[$label] 上传到 $server_user@$server_host:$remote_dir"
  run_ssh_on "$server_user@$server_host" "mkdir -p '$remote_dir'"
  run_ssh_on "$server_user@$server_host" \
    "rm -rf '$remote_dir/index.html' '$remote_dir/favicon.ico' '$remote_dir/static'"
  run_scp_to "$server_user@$server_host:$remote_dir/" -r -p -q "$dist"/*
}

# 后端：JDK 8 打包 → 上传 jar → 远程 docker_build.sh 重建容器
deploy_back() {
  local server_host="$BACKEND_SERVER_HOST"
  local server_user="${BACKEND_SERVER_USER:-blog}"
  local jar_name="domerry-blog.jar"
  local local_jar="$PROJECT_ROOT/blog/domerry-server/target/$jar_name"

  # 项目是 Java 8 + 旧版 Lombok，JDK 16+ 编译会报 IllegalAccessError，固定用 JDK 8 打包
  local build_java_home
  build_java_home="${BUILD_JAVA_HOME:-$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)}"
  if [[ -n "$build_java_home" ]]; then
    export JAVA_HOME="$build_java_home"
  fi

  step "[后端] 打包（mvn clean package）"
  for command_name in mvn ssh scp; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "缺少命令：$command_name" >&2; exit 127; }
  done
  (cd "$PROJECT_ROOT/blog" && if [[ "$DRY_RUN" == true ]]; then echo "[dry-run] mvn clean package"; else mvn clean package; fi)
  if [[ "$DRY_RUN" != true ]]; then
    [[ -f "$local_jar" ]] || die "打包产物不存在：$local_jar"
  fi

  ensure_channel "$server_user" "$server_host"
  step "[后端] 上传 jar 到 $server_user@$server_host:$REMOTE_JAR_DIR 并重建容器"
  # 目录必须已存在，脚本不创建（mkdir 会导致失败）
  run_scp_to "$server_user@$server_host:$REMOTE_JAR_DIR/$jar_name" -q "$local_jar"
  run_ssh_on "$server_user@$server_host" \
    "cd '$REMOTE_DEPLOY_SCRIPT_DIR' && bash docker_build.sh"
}

# ---- 发布前先校验配置、确认部署通道，避免构建完才卡在密码输入 ----
need_front=false
need_back=false
for item in "${PLAN[@]}"; do
  if [[ "$item" == admin || "$item" == web ]]; then need_front=true; fi
  if [[ "$item" == back ]]; then need_back=true; fi
done
if [[ "$need_front" == true ]]; then
  : "${FRONTEND_SERVER_HOST:?"未配置前端目标服务器：export FRONTEND_SERVER_HOST=...，或填写 deploy.env"}"
fi
if [[ "$need_back" == true ]]; then
  : "${BACKEND_SERVER_HOST:?"未配置后端目标服务器：export BACKEND_SERVER_HOST=...，或填写 deploy.env"}"
fi
if [[ "$DRY_RUN" != true ]]; then
  if [[ "$need_front" == true ]]; then
    ensure_channel "${FRONTEND_SERVER_USER:-root}" "$FRONTEND_SERVER_HOST"
  fi
  if [[ "$need_back" == true ]]; then
    ensure_channel "${BACKEND_SERVER_USER:-blog}" "$BACKEND_SERVER_HOST"
  fi
fi

# ---- 执行 ----
for item in "${PLAN[@]}"; do
  echo "==================== $item ===================="
  case "$item" in
    admin|web) deploy_front "$item" ;;
    back) deploy_back ;;
  esac
done
step "全部完成（${PLAN[*]}）"
