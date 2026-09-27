package com.neki.admin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// :core 와 :domain 은 com.neki.admin 밖에 있으므로, admin 이 실제로 쓰는 범위만 지정한다.
// :domain 은 스캔하지 않는다. 전체를 열면 인프라 모듈을 요구하는 다른 도메인 서비스까지 딸려 와 기동이 실패한다.
// admin 이 쓰는 도메인 서비스는 DomainServiceConfig 에 명시적으로 등록한다.
//
// spring-security 는 :domain 의 런타임 의존성으로 전이되어 자동설정이 모든 요청을 막는다.
// 어드민 인증 도입(docs/superpowers/plans/2026-08-16-admin-auth.md) 전까지 시큐리티 자동설정을 제외한다.
@SpringBootApplication(
    scanBasePackages = ["com.neki.admin", "com.neki.core"],
    exclude = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
    ],
)
// 엔티티는 :domain·:core 에 있고, JPA 리포지터리는 admin 의 infra 에만 둔다.
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.admin")
class AdminApplication

fun main(args: Array<String>) {
    runApplication<AdminApplication>(*args)
}
