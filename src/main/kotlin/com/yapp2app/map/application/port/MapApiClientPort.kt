package com.yapp2app.map.application.port

import com.yapp2app.map.application.contract.KakaoLocalSearchResponse

/**
 * fileName       : MapApiClientPort
 * author         : darren
 * date           : 2026. 1. 16. 13:12
 * description    :
 */
interface MapApiClientPort {
    fun kakaoSearchByKeyword(
        query: String,
        page: Int = 1,
        size: Int = 15,
        rect: String? = null,
    ): KakaoLocalSearchResponse
}
