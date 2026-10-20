#!/usr/bin/env bash
# 构建后端镜像并重启容器。
#   用法：bash docker_build.sh [容器名] [镜像名]
#   缺省：容器名与镜像名均为 domerry-blog
#   可覆盖环境变量：TAG（默认 1.0）、SERVER_PORT（默认 8900）、SPRING_PROFILES_ACTIVE、
#                    SECRET_DIR（宿主机密钥文件所在目录，给了就只读挂载、不烤进镜像）
# 需在构建上下文目录内执行（与本 Dockerfile 同级），目录内应有：
#   Dockerfile、domerry-blog.jar、application-local-secret.yml、target.crt（可选）
set -euo pipefail

BUILD_CONTEXT="${BUILD_CONTEXT:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
CONTAINER_NAME="${1:-domerry-blog}"
IMAGE_NAME="${2:-$CONTAINER_NAME}"
TAG="${TAG:-1.0}"
SERVER_PORT="${SERVER_PORT:-8900}"

command -v docker >/dev/null 2>&1 || { echo "缺少命令：docker" >&2; exit 127; }

cd "$BUILD_CONTEXT"
[[ -f domerry-blog.jar ]] || {
  echo "构建上下文缺少 domerry-blog.jar：$BUILD_CONTEXT" >&2
  exit 1
}
[[ -f application-local-secret.yml ]] || {
  echo "构建上下文缺少 application-local-secret.yml：$BUILD_CONTEXT" >&2
  exit 1
}

# 清掉同名旧容器（存在才动，避免 set -e 直接失败）
if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "存在容器 $CONTAINER_NAME，停止并删除"
  docker stop "$CONTAINER_NAME" >/dev/null
  docker rm "$CONTAINER_NAME" >/dev/null
fi

# 端口被别的容器占着时直接报错，而不是让 docker run 抛一堆看不懂的信息
PORT_HOLDER="$(docker ps --filter "publish=$SERVER_PORT" --format '{{.Names}}' | head -1 || true)"
if [[ -n "$PORT_HOLDER" ]]; then
  echo "端口 $SERVER_PORT 已被容器 $PORT_HOLDER 占用，请先停止/删除它（例如沿用旧名 cool-blog 的容器）" >&2
  exit 1
fi

# 始终重建镜像：jar 已由上传步骤覆盖，跳过构建会继续跑旧代码
echo "构建镜像 $IMAGE_NAME:$TAG"
docker build -t "$IMAGE_NAME:$TAG" .

# 构建后先自检密钥文件落点：Spring 的 spring.config.import 是 optional:，
# 路径对不上不会报错，只会在运行时静默用占位值（表现为「host 不可达」）。
# 路径直接读 Dockerfile 里的 ENV，避免脚本和镜像各写一份又跑偏。
SECRET_IN_IMAGE="$(docker image inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$IMAGE_NAME:$TAG" \
  | sed -n 's/^APP_LOCAL_SECRET_FILE=//p' | head -1)"
if [[ -z "$SECRET_IN_IMAGE" ]]; then
  echo "镜像里没有 APP_LOCAL_SECRET_FILE，无法确认密钥文件落点，请检查 Dockerfile" >&2
  exit 1
fi
if ! docker run --rm --entrypoint sh "$IMAGE_NAME:$TAG" -c "test -r '$SECRET_IN_IMAGE'" 2>/dev/null; then
  echo "镜像内读不到密钥文件：$SECRET_IN_IMAGE（spring.config.import 会静默忽略它）" >&2
  exit 1
fi
echo "密钥文件已就位：$SECRET_IN_IMAGE"

RUN_ARGS=(--name "$CONTAINER_NAME" -d -p "$SERVER_PORT:$SERVER_PORT" -e TZ=Asia/Shanghai)
# 密钥文件路径显式传一遍：镜像里的 ENV 是默认值，这里留一个可覆盖的口子
RUN_ARGS+=(-e "APP_LOCAL_SECRET_FILE=$SECRET_IN_IMAGE")
if [[ -n "${SPRING_PROFILES_ACTIVE:-}" ]]; then
  RUN_ARGS+=(-e "SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE")
fi
# 不想把密钥烤进镜像时，用 SECRET_DIR 指向宿主机上的目录，只读挂载覆盖掉镜像内那份
if [[ -n "${SECRET_DIR:-}" ]]; then
  RUN_ARGS+=(-v "$SECRET_DIR:$(dirname "$SECRET_IN_IMAGE"):ro")
fi

echo "启动容器 $CONTAINER_NAME（端口 $SERVER_PORT）"
docker run "${RUN_ARGS[@]}" "$IMAGE_NAME:$TAG"

echo "$CONTAINER_NAME 容器创建完成"
echo "看启动日志：docker logs -f $CONTAINER_NAME"
echo "首行会打印「已找到密钥文件」或「读不到密钥文件」，据此判断配置有没有生效"
