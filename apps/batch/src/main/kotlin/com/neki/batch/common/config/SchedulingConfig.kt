package com.neki.batch.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import java.time.ZoneId

@Configuration
@EnableScheduling
class SchedulingConfig {

    /** 운영 타임존 시계. businessDate 산출과 @Scheduled cron 의 zone 이 같은 값을 본다 */
    @Bean
    fun clock(@Value("\${neki.batch.zone:Asia/Seoul}") zone: String): Clock = Clock.system(ZoneId.of(zone))
}
