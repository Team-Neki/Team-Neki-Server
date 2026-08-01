plugins {
    kotlin("plugin.jpa")
}

dependencies {
    // api: neki-application 의 */infra/persist 어댑터가 JpaRepository·JPAQueryFactory 를 직접 사용한다.
    //      implementation 으로 낮추면 소비자 컴파일이 깨진다 (starter-data-jpa 74건, querydsl-jpa 635건).
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("com.querydsl:querydsl-jpa:5.0.0:jakarta")

    // Hibernate 가 dialect 로 로드하는 런타임 SPI 라 소비자 컴파일에 필요하지 않다.
    // 공간 엔티티(PhotoBoothLocation)는 :neki-domain 에 있고 거기서 따로 선언한다.
    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("org.locationtech.jts:jts-core:1.19.0")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
}
