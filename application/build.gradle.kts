plugins {
    kotlin("plugin.jpa")
}

val jwtVersion = "0.12.5"
val springDocVersion = "2.6.0"
val jtsVersion = "1.19.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":modules:discord"))
    implementation(project(":modules:s3"))
    implementation(project(":modules:redis"))

    implementation("org.locationtech.jts:jts-core:$jtsVersion")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.springframework:spring-web")
}
