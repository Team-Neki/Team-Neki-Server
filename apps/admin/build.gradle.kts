plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:postgres"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // 당분간 H2 로 운영한다. PostgreSQL 연결 설정은 후속 작업이다.
    runtimeOnly("com.h2database:h2")
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
