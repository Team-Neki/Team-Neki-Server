package com.neki.batch

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import kotlin.system.exitProcess

// one-shot 배치 프로세스. 기동하면 Boot 의 JobLauncherApplicationRunner 가 --spring.batch.job.name 으로
// 지정한 Job 을 실행하고, Job 상태를 종료 코드로 남기며 끝난다 (COMPLETED=0, 그 외 0 이 아님).
// Prefect flow 가 k8s Job 으로 이 이미지를 띄우고 종료 코드로 성공/실패를 판단한다.
//
//   java -jar neki-batch.jar --spring.batch.job.name=searchIndexJob businessDate=2026-09-25
//
// 스캔 범위는 필요한 것만으로 좁힌다. com.neki 전체를 스캔하면 :domain 의 user/infra/security 가
// apps/api 에만 있는 CorsConfigurationSource 를 요구하고, modules 의 aws/redis/firebase 연결 설정이
// 각자의 yaml 과 외부 시스템을 요구해 기동이 깨진다.
// searchIndexJob 이 쓰는 search 도메인과 map 의 persist 어댑터(BrandRepository)만 더 스캔한다.
// map 의 kakao/infra 는 외부 API 설정을 요구하므로 일부러 넣지 않는다.
@SpringBootApplication(
    scanBasePackages = [
        "com.neki.batch",
        "com.neki.core",
        "com.neki.config.postgres",
        "com.neki.config.jasypt",
        "com.neki.domain.search",
        "com.neki.domain.map.infra.persist",
    ],
    exclude = [
        // :domain 의 spring-security 가 runtime classpath 에 전이로 올라온다. 쓰지 않으므로 기본 사용자 생성을 막는다
        SecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        // application-redis.yaml 을 import 하지 않으므로 localhost 로 향하는 커넥션 팩토리를 만들지 않는다
        RedisAutoConfiguration::class,
        RedisRepositoriesAutoConfiguration::class,
    ],
)
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.domain")
class NekiBatchApplication {

    /**
     * 마이그레이션 소유는 api 다. batch 는 DB 스키마가 자기가 아는 마이그레이션과 맞는지 검증만 하고
     * flyway_schema_history 에 쓰지 않는다.
     *
     * migrate 를 허용하면 배포 워크플로의 ref 입력으로 올린 브랜치 이미지가 브랜치에만 있는 마이그레이션을
     * 운영 history 에 남기고, 그 뒤 main 에 다른 내용의 같은 버전이 들어오면 api 가 checksum mismatch 로
     * 기동하지 못한다. validate 만 하면 그런 브랜치는 "resolved migration not applied" 로 여기서 멈춘다.
     * 아직 api 가 적용하지 않은 마이그레이션이 있어도 같은 이유로 멈추므로, batch 는 api 배포 뒤에 올린다.
     */
    @Bean
    fun flywayValidateOnly(): FlywayMigrationStrategy = FlywayMigrationStrategy { it.validate() }
}

fun main(args: Array<String>) {
    val context: ConfigurableApplicationContext = runApplication<NekiBatchApplication>(*args)
    // Boot 가 등록한 JobExecutionExitCodeGenerator 가 Job 상태를 종료 코드로 바꾼다.
    // exit() 를 거쳐야 그 값이 JVM 종료 코드가 된다. 그냥 main 이 끝나면 실패해도 0 으로 끝난다
    exitProcess(SpringApplication.exit(context))
}
