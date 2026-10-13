package com.neki.domain.map.infra.persist

import com.neki.domain.map.infra.persist.jpa.PhotoBoothQueryRepository
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.models.PhotoBoothWithDistance
import com.neki.domain.map.repository.PhotoBoothRepository
import org.locationtech.jts.geom.Coordinate
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoBoothRepositoryAdapter
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스(tb_photo_booth_enriched) 읽기 Adapter. 테이블은 Workflow 소유라 쓰지 않는다
 */
@Repository
class PhotoBoothRepositoryAdapter(private val queryRepository: PhotoBoothQueryRepository) : PhotoBoothRepository {

    override fun existsByKey(key: PhotoBoothKey): Boolean = queryRepository.existsByKey(key)

    override fun findExistingKeys(keys: Collection<PhotoBoothKey>): Set<PhotoBoothKey> =
        queryRepository.findExistingKeys(keys)

    override fun findByKeys(keys: Collection<PhotoBoothKey>): List<PhotoBoothView> = queryRepository.findByKeys(keys)

    override fun listPolygonPhotoBooths(coordinates: List<Coordinate>, brandIds: List<Long>?): List<PhotoBoothView> =
        queryRepository.findByPolygon(coordinates, brandIds)

    override fun listPointPhotoBooths(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
    ): List<PhotoBoothWithDistance> = queryRepository.findByDistanceFromPoint(coordinate, radiusInMeters, brandIds)
}
