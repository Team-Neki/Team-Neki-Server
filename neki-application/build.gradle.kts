plugins {
    kotlin("plugin.jpa")
    id("org.springframework.boot")
}

val jwtVersion = "0.12.5"
val springDocVersion = "2.6.0"
val jtsVersion = "1.19.0"
val logstashEncoderVersion = "8.0"

dependencies {
    implementation(project(":neki-core"))
    implementation(project(":neki-domain"))
    implementation(project(":modules:discord"))
    implementation(project(":modules:s3"))
    implementation(project(":modules:redis"))
    implementation(project(":modules:kakao"))
    implementation(project(":modules:postgres"))
    implementation(project(":modules:jasypt"))
    implementation(project(":modules:apple"))

    implementation("org.locationtech.jts:jts-core:$jtsVersion")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwtVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.springframework:spring-web")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")

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
