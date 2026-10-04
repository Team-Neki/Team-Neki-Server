package com.neki.api.search.api.dto

import com.neki.api.search.application.dto.SearchResult
import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.Pagination
import com.neki.core.exception.BusinessException
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.UserLocation
import com.neki.domain.search.service.qu.SearchNormalizer
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
                keyword = SearchNormalizer.collapseSpaces(keyword),
                pagination = Pagination(page = page, size = size),
            )

        fun toSearchStationsQuery(
            keyword: String,
            page: Int,
            size: Int,
            latitude: Double?,
            longitude: Double?,
        ): SearchQuery.SearchStations = SearchQuery.SearchStations(
            keyword = SearchNormalizer.collapseSpaces(keyword),
            pagination = Pagination(page = page, size = size),
            userLocation = toUserLocation(latitude, longitude),
        )

        fun toSearchPhotoBoothsByKeywordQuery(
            keyword: String,
            page: Int,
            size: Int,
            latitude: Double?,
            longitude: Double?,
        ): SearchQuery.SearchPhotoBoothsByKeyword = SearchQuery.SearchPhotoBoothsByKeyword(
            keyword = SearchNormalizer.collapseSpaces(keyword),
            pagination = Pagination(page = page, size = size),
            userLocation = toUserLocation(latitude, longitude),
        )

        fun toGetPhotoBoothsQuery(userId: Long, request: SearchRequest.GetPhotoBooths): SearchQuery.GetPhotoBooths =
            SearchQuery.GetPhotoBooths(
                userId = userId,
                keyword = request.keyword!!,
                brandIds = toBrandIds(request.filterGroup),
                userLocation = request.userLocation?.let {
                    UserLocation(latitude = it.latitude!!, longitude = it.longitude!!)
                },
            )

        fun toGetFilterQuery(userId: Long, request: SearchRequest.GetFilter): SearchQuery.GetFilter =
            SearchQuery.GetFilter(
                userId = userId,
                keyword = request.keyword!!,
                brandIds = toBrandIds(request.filterGroup),
            )

        private fun toBrandIds(filterGroup: SearchRequest.FilterGroup): List<Long>? =
            filterGroup.brandFilter?.brands?.map { it.brandId }

        /**
         * 위도와 경도는 둘 다 있거나 둘 다 없어야 한다. 하나만 오면 D-01.
         */
        private fun toUserLocation(latitude: Double?, longitude: Double?): UserLocation? = when {
            latitude == null && longitude == null -> null
            latitude != null && longitude != null -> UserLocation(latitude = latitude, longitude = longitude)
            else -> throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }

    @Component
    class ResponseConverter {
        fun toCompletionResponse(result: SearchResult.Completion): SearchResponse.Completion =
            SearchResponse.Completion(
                items = result.items.map {
                    SearchResponse.Completion.Item(keyword = it.keyword, distanceKm = it.distanceKm)
                },
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
