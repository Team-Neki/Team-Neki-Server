package com.yapp2app.photo.infra.persist.jpa

import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.photo.domain.entity.PhotoImage
import com.yapp2app.photo.domain.entity.QPhotoImage.photoImage
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoImageQueryRepository
 * author         : koo
 * date           : 2026. 1. 14.
 * description    : PhotoImage QueryDSL Repository for pagination
 */
@Repository
class PhotoImageQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findOwnedPhotos(userId: Long, folderId: Long?, offset: Int, limit: Int): List<PhotoImage> = queryFactory
        .selectFrom(photoImage)
        .where(
            photoImage.userId.eq(userId),
            folderId?.let { photoImage.folderId.eq(it) },
        )
        .orderBy(photoImage.createdAt.desc())
        .offset(offset.toLong())
        .limit(limit.toLong())
        .fetch()
}
