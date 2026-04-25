plugins {
    id("java")
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Swagger (Boot 3.3.x bilan mos)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ✅ Redis Cache (Spring Boot 3.3.x compatible)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // ✅ PRODUCTION: Rate Limiting with Bucket4j + Redis
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("com.bucket4j:bucket4j-redis:8.10.1")

    // ✅ PRODUCTION: Bean Validation for request validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ✅ PRODUCTION: Actuator for health checks and monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Firebase Admin SDK
    implementation("com.google.firebase:firebase-admin:9.4.3")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MySQL
    runtimeOnly("com.mysql:mysql-connector-j:8.4.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")


    // ✅ SHU QATOR MUHIM (xatoni tuzatadi)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")


    implementation(platform("software.amazon.awssdk:bom:2.27.21"))
    implementation("software.amazon.awssdk:s3")

}
tasks.withType<Test> {
    useJUnitPlatform()
}

// ❗ BOOTJAR NI YOQAMIZ
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

tasks.getByName<Jar>("jar") {
    enabled = false
}
