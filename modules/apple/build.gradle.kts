val jwtVersion = "0.12.5"

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-web")
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
}
