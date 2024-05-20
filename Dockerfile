# Docker 镜像构建
# @author bh
# 打包maven3.5  jdk8
FROM maven:3.5-jdk-8-alpine as builder

# Copy local code to the container image.
# 工作目录
WORKDIR /app
# .表示当前目录 也就是app 把pom.xml复制到app下
COPY pom.xml .
# 同理把src文件夹复制到app/中
COPY src ./src

# Build a release artifact.
RUN mvn package -DskipTests

# Run the web service on container startup.
CMD ["java","-jar","/app/target/user-center-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]

