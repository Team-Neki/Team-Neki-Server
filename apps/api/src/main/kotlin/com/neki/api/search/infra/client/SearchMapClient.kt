package com.neki.api.search.infra.client

import com.neki.api.map.application.SearchPhotoBoothLocationsUseCase
import com.neki.api.map.application.dto.MapResult
import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.search.client.PhotoBoothClient
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.PhotoBoothSummary
import org.locationtech.jts.geom.Coordinate
import org.springframework.stereotype.Component

/**
 * fileName       : SearchMapClient
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : monolithic architecture map client
 * - map service 분리 시 OpenFeign, EventPublisher/Consumer로 변경
 */
@Component
class SearchMapClient(private val searchPhotoBoothLocationsUseCase: SearchPhotoBoothLocationsUseCase) :
    PhotoBoothClient {

    override fun searchByName(query: SearchQuery.SearchPhotoBoothsByKeyword): PageWithTotalCount<PhotoBoothSummary> {
        val result: MapResult.SearchPhotoBooths = searchPhotoBoothLocationsUseCase.execute(
            MapQuery.SearchPhotoBooths(
                keyword = query.keyword,
                pagination = query.pagination,
                coordinate = query.userLocation?.let { Coordinate(it.longitude, it.latitude) },
            ),
        )

        return PageWithTotalCount(
            items = result.locations.map {
                PhotoBoothSummary(
                    brandName = it.brandName,
                    branchName = it.branchName,
                    latitude = it.location.y,
                    longitude = it.location.x,
                )
            },
            hasNext = result.hasNext,
            totalCount = result.totalCount,
        )
    }
}
