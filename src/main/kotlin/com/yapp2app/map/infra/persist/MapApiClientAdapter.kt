package com.yapp2app.map.infra.persist

import com.yapp2app.map.application.contract.KakaoLocalSearchResponse
import com.yapp2app.map.application.port.MapApiClientPort
import com.yapp2app.map.infra.client.KakaoApiClient
import org.springframework.stereotype.Component

/**
 * fileName       : MapApiClientAdapter
 * author         : darren
 * date           : 2026. 1. 16. 13:14
 * description    :
 */
@Component
class MapApiClientAdapter(private val kakaoApiClient: KakaoApiClient) : MapApiClientPort {
    override fun kakaoSearchByKeyword(query: String, page: Int, size: Int, rect: String?): KakaoLocalSearchResponse =
        kakaoApiClient.searchByKeyword(query, page, size, rect)
}
