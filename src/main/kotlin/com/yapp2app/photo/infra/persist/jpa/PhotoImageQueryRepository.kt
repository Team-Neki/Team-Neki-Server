package com.yapp2app.photo.infra.persist.jpa

import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.photo.domain.entity.PhotoImage
import com.yapp2app.photo.domain.entity.QFavoritePhoto.favoritePhoto
import com.yapp2app.photo.domain.entity.QPhotoImage.photoImage
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoImageQueryRepository
 * author         : koo
 * date           : 2026. 1. 13. 오후 10:40
 * description    : PhotoImage QueryDSL Repository for pagination
 */
@Repository
class PhotoImageQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findOwnedPhotos(
        userId: Long,
        folderId: Long?,
        offset: Int,
        limit: Int,
        sortOrder: SortOrder,
    ): List<PhotoImage> = queryFactory.selectFrom(photoImage)
        .where(
            photoImage.userId.eq(userId),
            folderId?.let { photoImage.folderId.eq(it) },
        )
        .orderBy(
            when (sortOrder) {
                SortOrder.ASC -> photoImage.createdAt.asc()
                SortOrder.DESC -> photoImage.createdAt.desc()
            },
        )
        .offset(offset.toLong())
        .limit(limit.toLong())
        .fetch()

    fun findOwnedFavoritePhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage> =
        queryFactory.selectFrom(photoImage)
            .innerJoin(favoritePhoto)
            .on(
                favoritePhoto.id.userId.eq(photoImage.userId),
                favoritePhoto.id.photoId.eq(photoImage.id),
            )
            .where(
                photoImage.userId.eq(userId),
            )
            .orderBy(
                when (sortOrder) {
                    SortOrder.ASC -> photoImage.createdAt.asc()
                    SortOrder.DESC -> photoImage.createdAt.desc()
                },
            )
            .offset(offset.toLong())
            .limit(limit.toLong())
            .fetch()

    fun findLatestOwnedPhoto(userId: Long): PhotoImage? = queryFactory.selectFrom(photoImage)
        .where(
            photoImage.userId.eq(userId),
        )
        .orderBy(photoImage.createdAt.desc())
        .limit(1)
        .fetchOne()

    fun updatePhotosFolderIdToNull(userId: Long, folderIds: List<Long>): Int {
        if (folderIds.isEmpty()) return 0

        return queryFactory
            .update(photoImage)
            .setNull(photoImage.folderId)
            .where(photoImage.userId.eq(userId), photoImage.folderId.`in`(folderIds))
            .execute().toInt()
    }
}
