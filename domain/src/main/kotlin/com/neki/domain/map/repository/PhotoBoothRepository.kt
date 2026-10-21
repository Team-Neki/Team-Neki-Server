package com.neki.domain.map.repository

import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.models.PhotoBoothWithDistance
import org.locationtech.jts.geom.Coordinate

/**
 * fileName       : PhotoBoothRepository
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 지금 세대 포토부스(tb_photo_booth_enriched) 읽기. 지도 V2 와 검색이 쓴다.
 *   브랜드가 붙는 조회는 platform 과 같은 code 의 살아있는 브랜드가 있고 좌표가 있는 부스만 돌려준다
 */
interface PhotoBoothRepository {

    fun existsByKey(key: PhotoBoothKey): Boolean

    /** keys 중 지금 세대에 있는 키 */
    fun findExistingKeys(keys: Collection<PhotoBoothKey>): Set<PhotoBoothKey>

    fun findByKeys(keys: Collection<PhotoBoothKey>): List<PhotoBoothView>

    fun listPolygonPhotoBooths(coordinates: List<Coordinate>, brandIds: List<Long>?): List<PhotoBoothView>

    fun listPointPhotoBooths(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
    ): List<PhotoBoothWithDistance>
}
