plugins {
    id("org.springframework.boot")
}

val jwtVersion = "0.12.5"
val springDocVersion = "2.6.0"
val jtsVersion = "1.19.0"
val logstashEncoderVersion = "8.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:discord"))
    implementation(project(":modules:aws"))
    implementation(project(":modules:redis"))
    implementation(project(":modules:kakao"))
    implementation(project(":modules:postgres"))
    implementation(project(":modules:jasypt"))
    implementation(project(":modules:apple"))
    implementation(project(":modules:firebase"))

    implementation("org.locationtech.jts:jts-core:$jtsVersion")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwtVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.springframework:spring-web")

    // 컴파일 참조 없이 설정(management.*)과 logback-spring.xml 로만 활성화된다
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    testRuntimeOnly("com.h2database:h2")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

tasks.processResources {
    filesMatching("application.yaml") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf("version" to project.version.toString()),
        )
    }
}

tasks.jar { enabled = false }
tasks.bootJar { layered { enabled = true } }
