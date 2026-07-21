package com.neki.map.infra.client.fake

import com.neki.map.application.port.MapApiClientPort
import com.neki.map.application.port.dto.MapContract
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

    private val searchResults = ConcurrentHashMap<String, MapContract.LocalSearchResult>()

    override fun searchByKeyword(query: String, page: Int, size: Int, rect: String?): MapContract.LocalSearchResult =
        searchResults[query] ?: MapContract.LocalSearchResult(
            documents = emptyList(),
            searchPaginationMeta = MapContract.LocalSearchResult.SearchPaginationMeta(
                totalCount = 0,
                pageableCount = 0,
                isEnd = true,
            ),
        )
}
