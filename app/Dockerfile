# Simple Dockerfile — fallback cho khi Paketo Buildpacks không chạy ngon
# trên Apple Silicon + Colima. Build pipeline:
#   cd app && ./gradlew bootJar
#   docker build -t com.mbs/hub:0.1.0-SNAPSHOT .

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S hub && adduser -S hub -G hub

WORKDIR /app
COPY build/libs/*.jar app.jar

USER hub
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"
ENV TZ=Asia/Saigon

ENTRYPOINT exec java $JAVA_OPTS -jar /app/app.jar
