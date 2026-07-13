package com.neki.map.infra.persist.jpa

import com.neki.map.application.contract.PhotoBoothLocationDto
import com.neki.map.domain.entity.QBrand.brand
import com.neki.map.domain.entity.QFavoriteMap.favoriteMap
import com.neki.map.domain.entity.QPhotoBoothLocation.photoBoothLocation
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : FavoriteMapQueryRepository
 * author         : darren
 * date           : 2026. 6. 21.
 * description    : 즐겨찾기한 포토부스 조회 QueryDSL Repository
 */
@Repository
class FavoriteMapQueryRepository(private val queryFactory: JPAQueryFactory) {

    /**
     * 사용자가 즐겨찾기한 포토부스를 최근 즐겨찾기한 순서대로 조회
     * @param userId 사용자 ID
     */
    fun findFavoriteLocationsByUserId(userId: Long): List<PhotoBoothLocationDto> = queryFactory
        .select(
            Projections.constructor(
                PhotoBoothLocationDto::class.java,
                photoBoothLocation.id,
                brand.name,
                photoBoothLocation.branchName,
                photoBoothLocation.address,
                photoBoothLocation.location,
            ),
        )
        .from(favoriteMap)
        .join(photoBoothLocation).on(photoBoothLocation.id.eq(favoriteMap.id.locationId))
        .leftJoin(brand).on(brand.id.eq(photoBoothLocation.brandId))
        .where(favoriteMap.id.userId.eq(userId))
        .orderBy(favoriteMap.createdAt.desc())
        .fetch()

    /**
     * 주어진 위치 ID 중 사용자가 즐겨찾기한 위치 ID 목록 조회
     * @param userId 사용자 ID
     * @param locationIds 조회 범위로 제한할 위치 ID 목록
     */
    fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): List<Long> {
        if (locationIds.isEmpty()) return emptyList()

        return queryFactory
            .select(favoriteMap.id.locationId)
            .from(favoriteMap)
            .where(
                favoriteMap.id.userId.eq(userId),
                favoriteMap.id.locationId.`in`(locationIds),
            )
            .fetch()
    }
}
