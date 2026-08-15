plugins {
    kotlin("plugin.jpa")
    kotlin("kapt")
}

val jwtVersion = "0.12.5"

dependencies {
    implementation(project(":core"))
    // infra/persist, infra/cache, infra/storage, infra/security 기술 어댑터가 각 모듈의
    // 연결 설정(S3Properties, DiscordProperties)과 클라이언트를 직접 사용한다
    implementation(project(":modules:aws"))
    implementation(project(":modules:redis"))
    implementation(project(":modules:discord"))
    implementation(project(":modules:firebase"))
    implementation(project(":modules:kakao"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("org.locationtech.jts:jts-core:1.19.0")
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
}
