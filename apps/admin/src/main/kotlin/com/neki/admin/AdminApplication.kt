package com.neki.admin

import com.neki.config.aws.S3Properties
import com.neki.domain.media.infra.storage.fake.FakeMediaStorageConfig
import com.neki.domain.media.infra.storage.s3.S3MediaStorageAdapterConfig
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// :core 와 :domain 은 com.neki.admin 밖에 있으므로, admin 이 실제로 쓰는 범위만 지정한다.
// :domain 은 스캔하지 않는다. 전체를 열면 인프라 모듈을 요구하는 다른 도메인 서비스까지 딸려 와 기동이 실패한다.
// 도메인 서비스는 각 기능의 설정 클래스에 명시적으로 등록한다.
//
// spring-security 는 :domain 의 런타임 의존성으로 전이되어 자동설정이 모든 요청을 막는다.
// 어드민 인증 도입(docs/superpowers/plans/2026-08-16-admin-auth.md) 전까지 시큐리티 자동설정을 제외한다.
@SpringBootApplication(
    scanBasePackages = [
        "com.neki.admin",
        "com.neki.core",
        "com.neki.config.postgres",
        "com.neki.config.jasypt",
        "com.neki.config.aws",
    ],
    exclude = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
    ],
)
// 엔티티는 :domain·:core 에 있고, JPA 리포지터리는 admin 의 infra 에만 둔다.
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.admin")
@EnableConfigurationProperties(S3Properties::class)
@Import(S3MediaStorageAdapterConfig::class, FakeMediaStorageConfig::class)
class AdminApplication {
    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()

    // API 가 공유 스키마의 테이블을 생성·변경한다. admin 은 기존 마이그레이션 이력만 검증한다.
    @Bean
    fun flywayValidateOnly(): FlywayMigrationStrategy = FlywayMigrationStrategy { it.validate() }
}

fun main(args: Array<String>) {
    runApplication<AdminApplication>(*args)
}
