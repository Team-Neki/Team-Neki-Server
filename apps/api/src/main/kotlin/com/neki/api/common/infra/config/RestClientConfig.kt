package com.neki.api.common.infra.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

/**
 * fileName       : RestClientConfig
 * author         : darren
 * date           : 2025. 12. 28. 23:43
 * description    : RestClient Bean 등록
 */
@Configuration
class RestClientConfig {

    @Bean
    fun restClient(): RestClient = RestClient.builder()
        .build()
}
