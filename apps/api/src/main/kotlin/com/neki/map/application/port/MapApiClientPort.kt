package com.neki.map.application.port

import com.neki.map.application.port.dto.MapContract

/**
 * fileName       : MapApiClientPort
 * author         : darren
 * date           : 2026. 1. 16. 13:12
 * description    :
 */
interface MapApiClientPort {
    fun searchByKeyword(
        query: String,
        page: Int = 1,
        size: Int = 15,
        rect: String? = null,
    ): MapContract.LocalSearchResult
}
