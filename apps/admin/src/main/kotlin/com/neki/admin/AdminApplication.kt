package com.neki.admin

import com.neki.config.aws.S3Properties
import com.neki.domain.media.infra.storage.fake.FakeMediaStorageConfig
import com.neki.domain.media.infra.storage.s3.S3MediaStorageAdapterConfig
import com.neki.domain.user.infra.security.admin.AdminSecurityConfig
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// :core 와 :domain 은 com.neki.admin 밖에 있으므로, admin 이 실제로 쓰는 범위만 지정한다.
// :domain 은 스캔하지 않는다. 전체를 열면 인프라 모듈을 요구하는 다른 도메인 서비스까지 딸려 와 기동이 실패한다.
// 도메인 서비스는 각 기능의 설정 클래스에 명시적으로 등록한다.
@SpringBootApplication(
    scanBasePackages = [
        "com.neki.admin",
        "com.neki.core",
        "com.neki.config.postgres",
        "com.neki.config.jasypt",
        "com.neki.config.aws",
    ],
)
// admin 기능 리포지터리와 기존 사용자 JPA 리포지터리만 등록한다.
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.admin", "com.neki.domain.user.infra.persist.jpa")
@EnableConfigurationProperties(S3Properties::class)
@Import(S3MediaStorageAdapterConfig::class, FakeMediaStorageConfig::class, AdminSecurityConfig::class)
class AdminApplication {
    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()

    // 공유 DB DDL은 api만 적용한다. 미적용·불일치 스키마에서는 admin 기동을 거부한다.
    @Bean
    fun flywayValidateOnly(): FlywayMigrationStrategy = FlywayMigrationStrategy { it.validate() }
}

fun main(args: Array<String>) {
    runApplication<AdminApplication>(*args)
}
