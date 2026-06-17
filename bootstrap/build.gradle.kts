plugins {
    id("org.springframework.boot")
}

val jwtVersion = "0.12.5"
val springDocVersion = "2.6.0"
val jasyptVersion = "3.0.5"
val logstashEncoderVersion = "8.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":modules:postgres"))
    implementation(project(":modules:redis"))
    implementation(project(":modules:s3"))
    implementation(project(":modules:kakao"))
    implementation(project(":modules:discord"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwtVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:$jasyptVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")

    testRuntimeOnly("com.h2database:h2")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.locationtech.jts:jts-core:1.19.0")
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
