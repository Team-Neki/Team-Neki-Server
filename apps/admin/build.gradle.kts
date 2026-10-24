plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:postgres"))
    implementation(project(":modules:jasypt"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.session:spring-session-jdbc")
    implementation("org.flywaydb:flyway-core")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.22")

    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.testcontainers:postgresql")
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }

tasks.named<Test>("test") {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
