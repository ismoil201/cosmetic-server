FROM gradle:8.2.1-jdk17 AS build
WORKDIR /app

# 🔥 CACHE BUST (ENG MUHIM QATOR)
ARG CACHEBUST=1

COPY . .

# 🔥 CLEAN BUILD (eski class va DTO’larni o‘chiradi)
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
