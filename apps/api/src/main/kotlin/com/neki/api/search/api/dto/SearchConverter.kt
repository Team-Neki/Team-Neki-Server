package com.neki.api.search.api.dto

import com.neki.api.search.application.dto.SearchResult
import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.Pagination
import com.neki.core.exception.BusinessException
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.UserLocation
import org.springframework.stereotype.Component

/**
 * fileName       : SearchConverter
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:42
 * description    : Search api layer converter
 */
object SearchConverter {
    @Component
    class RequestConverter {
        fun toSearchRegionsQuery(keyword: String, page: Int, size: Int): SearchQuery.SearchRegions =
            SearchQuery.SearchRegions(
                keyword = keyword.normalizeSpaces(),
                pagination = Pagination(page = page, size = size),
            )

        fun toSearchStationsQuery(keyword: String, page: Int, size: Int): SearchQuery.SearchStations =
            SearchQuery.SearchStations(
                keyword = keyword.normalizeSpaces(),
                pagination = Pagination(page = page, size = size),
            )

        fun toGetPhotoBoothsQuery(userId: Long, request: SearchRequest.FilterGroup): SearchQuery.GetPhotoBooths =
            SearchQuery.GetPhotoBooths(
                userId = userId,
                target = toTarget(request),
                brandIds = request.brandFilter?.brandIds,
                userLocation = request.userLocation?.let {
                    UserLocation(latitude = it.latitude!!, longitude = it.longitude!!)
                },
            )

        fun toGetFilterQuery(userId: Long, request: SearchRequest.FilterGroup): SearchQuery.GetFilter =
            SearchQuery.GetFilter(
                userId = userId,
                target = toTarget(request),
                brandIds = request.brandFilter?.brandIds,
            )

        /**
         * 저장된 지역·역 이름은 낱말 사이가 공백 한 칸이라, 검색어도 앞뒤를 자르고 연속 공백을 한 칸으로 맞춘다.
         * DB 쪽 값을 가공하지 않아야 접두 검색이 인덱스를 탄다.
         */
        private fun String.normalizeSpaces(): String = trim().replace(WHITESPACES, " ")

        /**
         * regionFilter 와 stationFilter 중 정확히 하나만 있어야 한다. 둘 다 없거나 둘 다 있으면 D-01.
         */
        private fun toTarget(request: SearchRequest.FilterGroup): SearchTarget {
            val region: SearchRequest.FilterGroup.RegionFilter? = request.regionFilter
            val station: SearchRequest.FilterGroup.StationFilter? = request.stationFilter
            return when {
                region != null && station == null -> SearchTarget.Region(code = region.code!!)
                station != null && region == null -> SearchTarget.Station(
                    name = station.name!!,
                    lineName = station.lineName!!,
                )
                else -> throw BusinessException(ResultCode.INVALID_PARAMETER)
            }
        }

        companion object {
            private val WHITESPACES = Regex("\\s+")
        }
    }

    @Component
    class ResponseConverter {
        fun toCompletionResponse(result: SearchResult.Completion): SearchResponse.Completion =
            SearchResponse.Completion(
                items = result.keywords.map { SearchResponse.Completion.Item(keyword = it) },
                hasNext = result.hasNext,
                totalCount = result.totalCount,
            )

        fun toGetPhotoBoothsResponse(result: SearchResult.GetPhotoBooths): SearchResponse.GetPhotoBooths {
            val items: List<SearchResponse.GetPhotoBooths.Item> = result.items.map {
                SearchResponse.GetPhotoBooths.Item(
                    id = it.id,
                    brandName = it.brandName,
                    brandCode = it.brandCode,
                    branchName = it.branchName,
                    address = it.address,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    distance = it.distance,
                    favorite = it.favorite,
                )
            }
            return SearchResponse.GetPhotoBooths(items = items)
        }

        fun toGetFilterResponse(result: SearchResult.GetFilter): SearchResponse.GetFilter {
            val brandFilter: List<SearchResponse.GetFilter.BrandFilter> = result.brandFilter.map {
                SearchResponse.GetFilter.BrandFilter(id = it.id, name = it.name, code = it.code, count = it.count)
            }
            return SearchResponse.GetFilter(brandFilter = brandFilter)
        }
    }
}
