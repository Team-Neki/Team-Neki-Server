package com.neki.map

import com.neki.map.models.SearchedPlace

/**
 * fileName       : MapSearch
 * author         : darren
 * date           : 2026. 1. 22. 22:13
 * description    :
 */
interface MapSearch {
    fun searchAllKorea(keyword: String): List<SearchedPlace>
}
