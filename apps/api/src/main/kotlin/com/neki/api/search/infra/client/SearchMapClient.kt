package com.neki.api.search.infra.client

import com.neki.api.map.application.GetBrandUseCase
import com.neki.api.map.application.GetPhotoBoothUseCase
import com.neki.api.map.application.dto.MapResult
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.search.client.MapClient
import com.neki.domain.search.models.MapBooth
import com.neki.domain.search.models.PhotoBoothSearch
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
    private val getPhotoBoothUseCase: GetPhotoBoothUseCase,
    private val getBrandUseCase: GetBrandUseCase,
) : MapClient {

    override fun findMapBooths(userId: Long, indexedBooths: List<PhotoBoothSearch>): List<MapBooth> {
        if (indexedBooths.isEmpty()) return emptyList()

        val result: MapResult.GetPhotoBoothsByKeys = getPhotoBoothUseCase.execute(
            MapQuery.GetPhotoBoothsByKeys(
                userId = userId,
                keys = indexedBooths.map { PhotoBoothKey(platform = it.platform, idx = it.idx) },
            ),
        )

        return result.photoBooths.map {
            MapBooth(
                platform = it.key.platform,
                idx = it.key.idx,
                id = it.key.toBoothId(),
                favorite = it.favorite,
            )
        }
    }

    override fun findOrderedBrandIds(userId: Long): List<Long> =
        getBrandUseCase.execute(MapQuery.GetBrand(userId)).map { it.id }
}
