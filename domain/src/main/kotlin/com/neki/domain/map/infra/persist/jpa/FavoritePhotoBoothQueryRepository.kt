package com.neki.domain.map.infra.persist.jpa

import com.neki.domain.map.models.FavoritedLocation
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.QBrand.brand
import com.neki.domain.map.models.QFavoritePhotoBooth.favoritePhotoBooth
import com.neki.domain.map.models.QPhotoBoothLocation.photoBoothLocation
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : FavoritePhotoBoothQueryRepository
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스 키 즐겨찾기를 지점 마스터 위치로 바꿔 읽는 QueryDSL Repository (지도 V1 용).
 *   원천 키가 같은 지점 마스터 위치와 uq_photo_booth_location_source (source_platform, source_idx) 로 조인한다
 */
@Repository
class FavoritePhotoBoothQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findFavoriteLocationsByUserId(userId: Long): List<FavoritedLocation> = queryFactory
        .select(
            photoBoothLocation.id,
            brand.name,
            photoBoothLocation.branchName,
            photoBoothLocation.address,
            photoBoothLocation.location,
            favoritePhotoBooth.createdAt,
        )
        .from(favoritePhotoBooth)
        .join(photoBoothLocation).on(sameSource())
        // 브랜드가 삭제된 위치는 브랜드명이 없으므로 뺀다
        .join(brand).on(brand.id.eq(photoBoothLocation.brandId), brand.exposeToMap.isTrue)
        .where(favoritePhotoBooth.id.userId.eq(userId))
        .fetch()
        .map { row ->
            FavoritedLocation(
                location = PhotoBoothLocationView(
                    id = row.get(photoBoothLocation.id)!!,
                    brandName = row.get(brand.name)!!,
                    branchName = row.get(photoBoothLocation.branchName)!!,
                    address = row.get(photoBoothLocation.address)!!,
                    location = row.get(photoBoothLocation.location)!!,
                ),
                favoritedAt = row.get(favoritePhotoBooth.createdAt)!!,
            )
        }

    fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): List<Long> {
        if (locationIds.isEmpty()) return emptyList()

        return queryFactory
            .select(photoBoothLocation.id)
            .from(favoritePhotoBooth)
            .join(photoBoothLocation).on(sameSource())
            .where(
                favoritePhotoBooth.id.userId.eq(userId),
                photoBoothLocation.id.`in`(locationIds),
            )
            .fetch()
    }

    private fun sameSource(): BooleanExpression = photoBoothLocation.sourcePlatform.eq(favoritePhotoBooth.id.platform)
        .and(photoBoothLocation.sourceIdx.eq(favoritePhotoBooth.id.idx))
}
