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
    // k8s 프로브(/actuator/health)와 수동 트리거 API 를 위한 최소 web 스택.
    // actuator 는 NekiBatchApplication 이 ManagementWebSecurityAutoConfiguration 을 참조하므로 컴파일 의존이다
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    testRuntimeOnly("com.h2database:h2")
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
