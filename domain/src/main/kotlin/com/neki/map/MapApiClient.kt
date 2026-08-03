package com.neki.map

import com.neki.map.models.SearchedPlaces

/**
 * fileName       : MapApiClient
 * author         : darren
 * date           : 2026. 1. 16. 13:12
 * description    :
 */
interface MapApiClient {
    fun searchByKeyword(query: String, page: Int = 1, size: Int = 15, rect: String? = null): SearchedPlaces
}
