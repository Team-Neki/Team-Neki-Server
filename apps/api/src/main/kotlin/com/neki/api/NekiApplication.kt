package com.neki.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// 앱 클래스가 com.neki.api 로 내려오면서 기본 스캔 범위가 좁아졌다.
// :core, :domain, :modules 는 com.neki 아래 별도 트리에 있으므로 명시적으로 지정한다.
// api 앱은 클래스패스의 모든 모듈을 사용하므로 루트를 그대로 쓴다.
@SpringBootApplication(scanBasePackages = ["com.neki"])
@ConfigurationPropertiesScan("com.neki")
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.api")
class NekiApplication

fun main(args: Array<String>) {
    runApplication<NekiApplication>(*args)
}
