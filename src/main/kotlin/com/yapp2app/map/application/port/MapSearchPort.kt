package com.yapp2app.map.application.port

import com.yapp2app.map.application.contract.LocalSearchResult

/**
 * fileName       : MapSearchPort
 * author         : darren
 * date           : 2026. 1. 22. 22:13
 * description    :
 */
interface MapSearchPort {
    fun searchAllKorea(keyword: String): List<LocalSearchResult.Place>
}
