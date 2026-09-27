plugins {
    id("org.springframework.boot")
}

val logstashEncoderVersion = "8.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:postgres"))
    implementation(project(":modules:jasypt"))

    implementation("org.springframework.boot:spring-boot-starter-batch")
    // FlywayMigrationStrategy 가 Flyway 타입을 직접 참조한다 (modules:postgres 는 implementation 이라 전이되지 않음)
    implementation("org.flywaydb:flyway-core")
    // one-shot 프로세스라 web/actuator 가 없다. :domain 이 starter-web 을 전이로 가져오지만
    // application.yaml 의 spring.main.web-application-type=none 으로 서버를 띄우지 않는다
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    testRuntimeOnly("com.h2database:h2")
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
