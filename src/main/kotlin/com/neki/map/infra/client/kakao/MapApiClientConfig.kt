package com.neki.map.infra.client.kakao

import com.neki.map.application.port.MapApiClientPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestClient

@Profile("!test")
@Configuration
class MapApiClientConfig {
    @Bean
    fun mapApiClient(@Value("\${kakao.api.key}") apiKey: String, restClient: RestClient): MapApiClientPort =
        MapApiClientAdapter(apiKey, restClient)
}
