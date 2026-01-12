package com.yapp2app.common.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * fileName       : ObjectMapperConfig
 * author         : darren
 * date           : 2025. 12. 28. 23:43
 * description    : ObjectMapper Bean 등록
 */
@Configuration
class ObjectMapperConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerKotlinModule() // Kotlin 파라미터 이름 인식을 위한 Kotlin Module 등록
            .findAndRegisterModules() // JavaTime 등 다른 모듈도 자동 등록
    }
}
