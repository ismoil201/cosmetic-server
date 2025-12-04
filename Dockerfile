# 1) JDK bilan build qilish bosqichi
FROM gradle:8.7-jdk17 AS builder
WORKDIR /app

COPY . .
RUN gradle bootJar --no-daemon

# 2) Run bosqichi (eng yengil)
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
