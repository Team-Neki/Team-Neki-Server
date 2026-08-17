plugins {
    kotlin("plugin.jpa")
    kotlin("kapt")
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("org.springframework.data:spring-data-jpa")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.22")
    // AppProperties 의 @ConfigurationProperties
    implementation("org.springframework.boot:spring-boot")

    // QueryDSL: generate QBaseTimeEntity for the @MappedSuperclass owned by :core
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
}
