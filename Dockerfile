# ---- Build stage ----
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

# faqat gradle wrapperlarni ruxsat berish
COPY gradlew .
COPY gradle gradle

# butun projectni nusxalash
COPY . .

# testlarni o‘tkazmasdan build qilish
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# ---- Run stage ----
FROM openjdk:17-jdk-slim
WORKDIR /app

# build bo‘lgan jarni olish
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
