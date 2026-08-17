package com.neki.domain.map.external

import com.neki.domain.map.models.SearchedPlace

/**
 * fileName       : MapSearch
 * author         : darren
 * date           : 2026. 1. 22. 22:13
 * description    :
 */
interface MapSearch {
    fun searchAllKorea(keyword: String): List<SearchedPlace>
}
