package com.yapp2app.map.infra.client.fake

import com.yapp2app.map.application.contract.LocalSearchResponse
import com.yapp2app.map.application.port.MapApiClientPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.concurrent.ConcurrentHashMap

/**
 * fileName       : FakeMapApiClientConfig
 * author         : darren
 * date           : 2026. 01. 22.
 * description    : test를 위한 Map API Client Config
 */
@Profile("test")
@Configuration
class FakeMapApiClientConfig {

    @Bean
    fun fakeMapApiClient(): MapApiClientPort = FakeMapApiClientAdapter()
}

class FakeMapApiClientAdapter : MapApiClientPort {

    private val searchResults = ConcurrentHashMap<String, LocalSearchResponse>()

    override fun searchByKeyword(query: String, page: Int, size: Int, rect: String?): LocalSearchResponse =
        searchResults[query] ?: LocalSearchResponse(
            documents = emptyList(),
            meta = LocalSearchResponse.Meta(
                totalCount = 0,
                pageableCount = 0,
                isEnd = true,
            ),
        )
}
