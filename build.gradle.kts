import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.0.10"

    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.6"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    idea
}

// Version Management
val jwtVersion = "0.12.5"
val nimbusVersion = "9.37.3"
val bouncyCastleVersion = "1.78"
val awsSdkVersion = "2.27.0"
val springDocVersion = "2.6.0"
val kotestVersion = "5.9.1"
val mockkVersion = "1.13.10"

group = "com.yapp2app"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwtVersion")

    // Apple JWT (nimbus-jose-jwt)
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")

    // BouncyCastle
    implementation("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")

    // AWS SDK V2 (V1은 deprecated)
    implementation("software.amazon.awssdk:aws-core:$awsSdkVersion")

    // OpenAPI/Swagger (SpringFox는 deprecated, SpringDoc만 사용)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-spring:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}


tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll(listOf("-Xjsr305=strict"))
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}