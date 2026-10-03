package com.neki.api.search.infra.client

import com.neki.api.map.application.GetBrandNamesUseCase
import com.neki.api.map.application.GetBrandUseCase
import com.neki.api.map.application.GetPhotoBoothLocationUseCase
import com.neki.api.map.application.SearchPhotoBoothLocationsUseCase
import com.neki.api.map.application.dto.MapResult
import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothSource
import com.neki.domain.search.client.MapClient
import com.neki.domain.search.client.PhotoBoothClient
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.CompletionKeyword
import com.neki.domain.search.models.MapBooth
import com.neki.domain.search.models.PhotoBoothSearch
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
class SearchMapClient(
    private val searchPhotoBoothLocationsUseCase: SearchPhotoBoothLocationsUseCase,
    private val getBrandNamesUseCase: GetBrandNamesUseCase,
    private val getPhotoBoothLocationUseCase: GetPhotoBoothLocationUseCase,
    private val getBrandUseCase: GetBrandUseCase,
) : PhotoBoothClient,
    MapClient {

    override fun searchByKeyword(query: SearchQuery.SearchPhotoBoothsByKeyword): PageWithTotalCount<PhotoBoothSummary> {
        val result: MapResult.SearchPhotoBooths = searchPhotoBoothLocationsUseCase.execute(
            MapQuery.SearchPhotoBooths(
                keyword = query.keyword,
                terms = CompletionKeyword.boothTerms(query.keyword),
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

    override fun findBrandNames(): List<String> = getBrandNamesUseCase.execute()

    override fun findMapBooths(userId: Long, indexedBooths: List<PhotoBoothSearch>): List<MapBooth> {
        if (indexedBooths.isEmpty()) return emptyList()

        val result: MapResult.GetSourceLocations = getPhotoBoothLocationUseCase.execute(
            MapQuery.GetSourceLocations(
                userId = userId,
                sources = indexedBooths.map { PhotoBoothSource(platform = it.platform, idx = it.idx) },
            ),
        )

        return result.locations.map {
            MapBooth(
                platform = it.source.platform,
                idx = it.source.idx,
                locationId = it.locationId,
                favorite = it.favorite,
            )
        }
    }

    override fun findOrderedBrandIds(userId: Long): List<Long> =
        getBrandUseCase.execute(MapQuery.GetBrand(userId)).map { it.id }
}
