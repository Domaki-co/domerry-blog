FROM openjdk:8-jre-alpine

# 构建上下文（本目录）需要的文件：
#   domerry-blog.jar             —— mvn clean package 产物（finalName 见 blog/pom.xml）
#   application-local-secret.yml —— 本地私密配置，由 Spring Boot 读取，不入库
#   target.crt                   —— 上游服务证书；不需要导入信任库时删掉对应 COPY/RUN
WORKDIR /opt/domerry

# 密钥文件的落点必须和 application-*.yml 里
# spring.config.import: optional:file:${APP_LOCAL_SECRET_FILE:/etc/domerry/application-local-secret.yml}
# 完全一致。改成 optional: 之后文件缺失不会报错，只会静默退回占位值
# （jdbc:mysql://192.168.1.100:3306），然后以「host 不可达」的样子暴露出来。
ENV APP_LOCAL_SECRET_FILE=/etc/domerry/application-local-secret.yml

# 1. 复制证书并导入 Java 默认信任库
# JDK 8 Alpine 的 JAVA_HOME 指向 jre 目录，cacerts 在 $JAVA_HOME/lib/security/ 下
# 默认口令 changeit，-noprompt 避免构建时阻塞
COPY target.crt /tmp/target.crt
RUN keytool -importcert \
    -alias upstream-target \
    -file /tmp/target.crt \
    -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit \
    -noprompt && \
    rm -f /tmp/target.crt

# 2. 密钥文件：落到 ENV 声明的路径（COPY 会自动创建 /etc/domerry 目录）
COPY application-local-secret.yml ${APP_LOCAL_SECRET_FILE}

# 3. 应用 jar：容器内统一叫 domerry-blog.jar
COPY domerry-blog.jar /opt/domerry/domerry-blog.jar

VOLUME /tmp
EXPOSE 8900

# 4. profile 由 Spring Boot 原生环境变量 SPRING_PROFILES_ACTIVE 控制，默认 test
#    （对应仓库自带的 application-test.yml），换环境时用 docker run -e SPRING_PROFILES_ACTIVE=dev 覆盖
ENV SPRING_PROFILES_ACTIVE=test

# 5. 启动前自检：先把「密钥文件到底读没读到」打到 docker logs 里，省得对着
#    「host 不可达」猜。检查不过也照样启动 —— 有些环境确实只用 -e 传变量。
#    JVM 参数必须放在 -jar 之前，否则会被当成应用参数传给 Spring Boot。
ENTRYPOINT ["/bin/sh", "-c", "\
if [ -r \"$APP_LOCAL_SECRET_FILE\" ]; then \
  echo \"[entrypoint] 已找到密钥文件 $APP_LOCAL_SECRET_FILE\"; \
else \
  echo \"[entrypoint] 警告：读不到密钥文件 $APP_LOCAL_SECRET_FILE\" >&2; \
  echo \"[entrypoint] spring.config.import 是 optional:，缺失会被静默忽略，改用占位值（连不上数据库/Redis）\" >&2; \
fi; \
exec java \
  -Xms256m -Xmx300m \
  -Xss512k \
  -XX:MaxMetaspaceSize=128m \
  -XX:CompressedClassSpaceSize=64m \
  -XX:ReservedCodeCacheSize=48m \
  -XX:MaxDirectMemorySize=64m \
  -XX:+UseSerialGC \
  -XX:+ExitOnOutOfMemoryError \
  -Duser.timezone=GMT+8 \
  -Dfile.encoding=UTF-8 \
  -jar /opt/domerry/domerry-blog.jar \"$@\"", "--"]
