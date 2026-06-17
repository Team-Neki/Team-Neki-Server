package com.neki.photo.infra.persist.jpa

import com.neki.photo.domain.entity.QFavoritePhoto.favoritePhoto
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : FavoritePhotoQueryRepository
 * author         : koo
 * date           : 2026. 1. 22.
 * description    : FavoritePhoto QueryDSL Repository for batch operations
 */
@Repository
class FavoritePhotoQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun deleteAllByUserIdAndPhotoIds(userId: Long, photoIds: List<Long>): Long = queryFactory.delete(favoritePhoto)
        .where(
            favoritePhoto.id.userId.eq(userId),
            favoritePhoto.id.photoId.`in`(photoIds),
        )
        .execute()
}
