plugins {
    id("org.springframework.boot")
}

val springDocVersion = "2.6.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:postgres"))
    implementation(project(":modules:jasypt"))
    implementation(project(":modules:aws"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

    // admin 은 공유 DB 의 마이그레이션을 검증만 한다.
    implementation("org.flywaydb:flyway-core")
    implementation("io.micrometer:micrometer-core")

    testRuntimeOnly("com.h2database:h2")
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
