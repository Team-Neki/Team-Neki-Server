package com.neki.map.application.port

import com.neki.map.application.port.dto.MapContract

/**
 * fileName       : MapSearchPort
 * author         : darren
 * date           : 2026. 1. 22. 22:13
 * description    :
 */
interface MapSearchPort {
    fun searchAllKorea(keyword: String): List<MapContract.LocalSearchResult.Place>
}
