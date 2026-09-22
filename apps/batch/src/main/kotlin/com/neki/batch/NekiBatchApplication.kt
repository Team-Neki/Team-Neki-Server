package com.neki.batch

import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// 스캔 범위를 필요한 것만으로 좁힌다. com.neki 전체를 스캔하면 :domain 의 user/infra/security 가
// apps/api 에만 있는 CorsConfigurationSource 를 요구하고, modules 의 aws/redis/firebase 연결 설정이
// 각자의 yaml 과 외부 시스템을 요구해 기동이 깨진다.
// 실제 잡이 도메인 서비스/어댑터를 쓰게 되면 그 도메인 패키지(e.g. com.neki.domain.photo)를 여기에 추가한다.
@SpringBootApplication(
    scanBasePackages = ["com.neki.batch", "com.neki.core", "com.neki.config.postgres", "com.neki.config.jasypt"],
    exclude = [
        // :domain 의 spring-security 가 runtime classpath 에 전이로 올라와 기본 보안 체인이 프로브를 401 로 막는다
        SecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        // application-redis.yaml 을 import 하지 않으므로 localhost 로 향하는 커넥션 팩토리를 만들지 않는다
        RedisAutoConfiguration::class,
        RedisRepositoriesAutoConfiguration::class,
    ],
)
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.domain")
class NekiBatchApplication

fun main(args: Array<String>) {
    runApplication<NekiBatchApplication>(*args)
}
