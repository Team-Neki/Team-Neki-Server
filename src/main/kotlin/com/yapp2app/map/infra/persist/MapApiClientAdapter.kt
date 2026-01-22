package com.yapp2app.map.infra.persist

import com.yapp2app.map.application.contract.KakaoLocalSearchResponse
import com.yapp2app.map.application.port.MapApiClientPort
import com.yapp2app.map.infra.client.MapApiClient
import org.springframework.stereotype.Component

/**
 * fileName       : MapApiClientAdapter
 * author         : darren
 * date           : 2026. 1. 16. 13:14
 * description    :
 */
@Component
class MapApiClientAdapter(private val mapApiClient: MapApiClient) : MapApiClientPort {
    override fun searchByKeyword(query: String, page: Int, size: Int, rect: String?): KakaoLocalSearchResponse =
        mapApiClient.searchByKeyword(query, page, size, rect)
}
