plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:postgres"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // :domain 이 두 의존성을 implementation 으로 잡아 전이되지 않는다.
    // modules:postgres 대신 직접 선언해 H2 로 띄우는 현재 구성을 유지한다.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")

    // 당분간 H2 로 운영한다. PostgreSQL 전환 시 modules:postgres 로 교체한다
    runtimeOnly("com.h2database:h2")
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
