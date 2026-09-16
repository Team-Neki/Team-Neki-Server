package com.neki.api.search.api.dto

import com.neki.api.search.application.dto.SearchResult
import com.neki.domain.search.dto.SearchQuery
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
        fun toGetPhotoBoothsQuery(
            userId: Long,
            keyword: String,
            request: SearchRequest.GetPhotoBooths,
        ): SearchQuery.GetPhotoBooths = SearchQuery.GetPhotoBooths(
            userId = userId,
            keyword = keyword.trim(),
            brandIds = toBrandIds(request.filterGroup),
            userLocation = request.userLocation?.let {
                UserLocation(latitude = it.latitude!!, longitude = it.longitude!!)
            },
        )

        fun toGetFilterQuery(userId: Long, keyword: String, request: SearchRequest.GetFilter): SearchQuery.GetFilter =
            SearchQuery.GetFilter(
                userId = userId,
                keyword = keyword.trim(),
                brandIds = toBrandIds(request.filterGroup),
            )

        private fun toBrandIds(filterGroup: SearchRequest.FilterGroup): List<Long>? =
            filterGroup.brandFilter?.brands?.map { it.brandId }
    }

    @Component
    class ResponseConverter {
        fun toGetPhotoBoothsResponse(result: SearchResult.GetPhotoBooths): SearchResponse.GetPhotoBooths {
            val photoBooths: List<SearchResponse.GetPhotoBooths.PhotoBooth> = result.items.map {
                SearchResponse.GetPhotoBooths.PhotoBooth(
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
            return SearchResponse.GetPhotoBooths(items = photoBooths)
        }

        fun toGetFilterResponse(result: SearchResult.GetFilter): SearchResponse.GetFilter {
            val brandFilter: List<SearchResponse.GetFilter.BrandFilter> = result.brandFilter.map {
                SearchResponse.GetFilter.BrandFilter(id = it.id, name = it.name, code = it.code, count = it.count)
            }
            return SearchResponse.GetFilter(brandFilter = brandFilter)
        }
    }
}
