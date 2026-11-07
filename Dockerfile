# One Dockerfile for all services:  docker build --build-arg SERVICE=order-service -t shop/order-service .
# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${SERVICE} -am -DskipTests package

# ---- runtime stage: JRE only, non-root, container-aware JVM ----
FROM eclipse-temurin:17-jre-jammy
ARG SERVICE
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build /app/${SERVICE}/target/${SERVICE}-*.jar app.jar
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
