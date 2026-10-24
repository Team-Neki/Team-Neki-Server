package com.neki.domain.map.infra.persist.jpa

import com.neki.domain.map.models.FavoritedLocation
import com.neki.domain.map.models.FavoritedPhotoBooth
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.QBrand.brand
import com.neki.domain.map.models.QFavoriteMap.favoriteMap
import com.neki.domain.map.models.QPhotoBoothLocation.photoBoothLocation
import com.querydsl.jpa.JPAExpressions
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
     * 사용자가 즐겨찾기한 포토부스와 즐겨찾기한 시각. 순서는 TB_FAVORITE_PHOTO_BOOTH 와 합친 뒤 MapService 가 정한다
     * @param userId 사용자 ID
     */
    fun findFavoriteLocationsByUserId(userId: Long): List<FavoritedLocation> = queryFactory
        .select(
            photoBoothLocation.id,
            brand.name,
            photoBoothLocation.branchName,
            photoBoothLocation.address,
            photoBoothLocation.location,
            favoriteMap.createdAt,
        )
        .from(favoriteMap)
        .join(photoBoothLocation).on(photoBoothLocation.id.eq(favoriteMap.id.locationId))
        .join(brand).on(brand.id.eq(photoBoothLocation.brandId), brand.exposeToMap.isTrue)
        .where(favoriteMap.id.userId.eq(userId))
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
                favoritedAt = row.get(favoriteMap.createdAt)!!,
            )
        }

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

    /**
     * 원천 키가 있는 지점 마스터 위치의 즐겨찾기를 키로 바꿔 조회. 원천 키가 없는 위치(어드민 직접 입력)는 enriched 에 없으므로 뺀다
     * @param userId 사용자 ID
     */
    fun findFavoritedPhotoBooths(userId: Long): List<FavoritedPhotoBooth> = queryFactory
        .select(photoBoothLocation.sourcePlatform, photoBoothLocation.sourceIdx, favoriteMap.createdAt)
        .from(favoriteMap)
        .join(photoBoothLocation).on(photoBoothLocation.id.eq(favoriteMap.id.locationId))
        .where(
            favoriteMap.id.userId.eq(userId),
            photoBoothLocation.sourcePlatform.isNotNull,
            photoBoothLocation.sourceIdx.isNotNull,
        )
        .fetch()
        .map { row ->
            FavoritedPhotoBooth(
                key = PhotoBoothKey(
                    platform = row.get(photoBoothLocation.sourcePlatform)!!,
                    idx = row.get(photoBoothLocation.sourceIdx)!!,
                ),
                favoritedAt = row.get(favoriteMap.createdAt)!!,
            )
        }

    /**
     * 원천 키가 key 인 지점 마스터 위치의 즐겨찾기 삭제
     * @param userId 사용자 ID
     * @param key 포토부스 키
     */
    fun deleteByPhotoBoothKey(userId: Long, key: PhotoBoothKey) {
        queryFactory
            .delete(favoriteMap)
            .where(
                favoriteMap.id.userId.eq(userId),
                favoriteMap.id.locationId.`in`(
                    JPAExpressions
                        .select(photoBoothLocation.id)
                        .from(photoBoothLocation)
                        .where(
                            photoBoothLocation.sourcePlatform.eq(key.platform),
                            photoBoothLocation.sourceIdx.eq(key.idx),
                        ),
                ),
            )
            .execute()
    }
}
