FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S hub && adduser -S hub -G hub
WORKDIR /app
COPY build/libs/*.jar app.jar
USER hub
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"
ENV TZ=Asia/Saigon
ENTRYPOINT exec java $JAVA_OPTS -jar /app/app.jar
