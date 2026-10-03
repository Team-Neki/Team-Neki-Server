package com.neki.api.search.infra.client

import com.neki.api.map.application.GetBrandUseCase
import com.neki.api.map.application.GetPhotoBoothLocationUseCase
import com.neki.api.map.application.dto.MapResult
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothSource
import com.neki.domain.search.client.MapClient
import com.neki.domain.search.models.MapBooth
import com.neki.domain.search.models.PhotoBoothSearch
import org.springframework.stereotype.Component

/**
 * fileName       : SearchMapClient
 * author         : koo
 * date           : 2026. 9. 30.
 * description    : monolithic architecture map client. map 유스케이스를 호출해 search 모델로 바꾼다.
 * - map service 분리 시 OpenFeign 등으로 변경
 */
@Component
class SearchMapClient(
    private val getPhotoBoothLocationUseCase: GetPhotoBoothLocationUseCase,
    private val getBrandUseCase: GetBrandUseCase,
) : MapClient {

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
