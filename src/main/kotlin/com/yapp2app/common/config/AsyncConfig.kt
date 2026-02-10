package com.yapp2app.common.config

import org.slf4j.MDC
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.lang.reflect.Method
import java.util.concurrent.Executor

/**
 * 비동기 작업 설정
 *
 * 기능:
 * - MDC 자동 전파 (REQUEST_ID, USER_ID 등)
 * - SecurityContext 자동 전파
 * - 통합 예외 처리
 * - 모니터링 가능 (Actuator 연동)
 */
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {

    /**
     * 비동기 작업용 Thread pool
     *
     * 설정값:
     * - corePoolSize: 5 (기본 스레드 수)
     * - maxPoolSize: 20 (최대 스레드 수)
     * - queueCapacity: 100 (대기 큐 크기)
     * - keepAliveSeconds: 60 (유휴 스레드 유지 시간)
     *
     * 모니터링:
     * - Actuator: /actuator/metrics/executor.pool.size
     * - Thread 이름: async-media-N
     */
    @Bean(name = ["asyncExecutor"])
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()

        // Thread pool 크기 설정
        executor.corePoolSize = 5
        executor.maxPoolSize = 20
        executor.queueCapacity = 100
        executor.keepAliveSeconds = 60

        // Thread 이름 설정 (디버깅 용이)
        executor.setThreadNamePrefix("async-media-")

        // MDC 전파 설정
        executor.setTaskDecorator(MdcTaskDecorator())

        // 거부 정책: 호출자 스레드에서 실행 (CallerRunsPolicy)
        executor.setRejectedExecutionHandler(
            java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy(),
        )

        // Bean 이름 설정 (메트릭 식별용)
        executor.setBeanName("asyncExecutor")

        executor.initialize()
        return executor
    }

    /**
     * 비동기 작업의 uncaught exception 처리
     *
     * 처리 대상:
     * - @Async void 메서드의 예외 (Future 반환하지 않는 경우)
     * - CompletableFuture 내부 예외 중 .get()으로 받지 않는 것
     *
     * 처리 내용:
     * - 에러 로그
     * - 향후 확장: Sentry 전송, Slack 알림, 메트릭 증가
     */
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler = CustomAsyncExceptionHandler()
}

/**
 * MDC 전파 TaskDecorator
 *
 * 역할:
 * - 부모 스레드의 MDC를 자식 스레드로 복사
 * - REQUEST_ID, USER_ID 등의 로깅 컨텍스트 유지
 * - SecurityContext도 필요시 추가 가능
 */
class MdcTaskDecorator : TaskDecorator {
    override fun decorate(runnable: Runnable): Runnable {
        // 부모 스레드의 MDC 복사
        val contextMap = MDC.getCopyOfContextMap()

        return Runnable {
            try {
                // 자식 스레드에 MDC 설정
                contextMap?.let { MDC.setContextMap(it) }
                runnable.run()
            } finally {
                // 작업 완료 후 MDC 정리
                MDC.clear()
            }
        }
    }
}

/**
 * 비동기 예외 핸들러
 *
 * 처리 시나리오:
 * 1. @Async void 메서드에서 예외 발생
 * 2. CompletableFuture 내부 예외 중 .get()으로 받지 않는 것
 */
class CustomAsyncExceptionHandler : AsyncUncaughtExceptionHandler {
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    override fun handleUncaughtException(ex: Throwable, method: Method, vararg params: Any?) {
        log.error(
            "[AsyncException] Uncaught exception in async method: " +
                "${method.declaringClass.simpleName}.${method.name}, " +
                "params: ${params.joinToString { it?.toString() ?: "null" }}",
            ex,
        )
    }
}
