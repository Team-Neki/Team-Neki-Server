package com.yapp2app.common.api.config

import com.fasterxml.jackson.databind.ObjectMapper
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
        val objectMapper = ObjectMapper()
        return objectMapper
    }
}
