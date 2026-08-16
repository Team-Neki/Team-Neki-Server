package com.neki.admin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// :core 와 :domain 은 com.neki.admin 밖에 있으므로, admin 이 실제로 쓰는 범위만 지정한다.
// :domain 은 스캔하지 않는다. 전체를 열면 인프라 모듈을 요구하는 다른 도메인 서비스까지 딸려 와 기동이 실패한다.
// admin 이 쓰는 도메인 서비스는 DomainServiceConfig 에 명시적으로 등록한다.
@SpringBootApplication(scanBasePackages = ["com.neki.admin", "com.neki.core"])
// 엔티티는 :domain·:core 에 있고, JPA 리포지터리는 admin 의 infra 에만 둔다.
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.admin")
class AdminApplication

fun main(args: Array<String>) {
    runApplication<AdminApplication>(*args)
}
