package com.neki.api.map.infra.client.fake

import com.neki.domain.map.external.MapApiClient
import com.neki.domain.map.models.SearchPagination
import com.neki.domain.map.models.SearchedPlaces
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
    fun fakeMapApiClient(): MapApiClient = FakeMapApiClientAdapter()
}

class FakeMapApiClientAdapter : MapApiClient {

    private val searchResults = ConcurrentHashMap<String, SearchedPlaces>()

    override fun searchByKeyword(query: String, page: Int, size: Int, rect: String?): SearchedPlaces =
        searchResults[query] ?: SearchedPlaces(
            places = emptyList(),
            pagination = SearchPagination(
                totalCount = 0,
                pageableCount = 0,
                isEnd = true,
            ),
        )
}
