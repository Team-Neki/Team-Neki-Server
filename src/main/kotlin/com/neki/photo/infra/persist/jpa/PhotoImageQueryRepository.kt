package com.neki.photo.infra.persist.jpa

import com.neki.common.domain.vo.SortOrder
import com.neki.photo.contract.PhotoWithFavorite
import com.neki.photo.domain.entity.PhotoImage
import com.neki.photo.domain.entity.QFavoritePhoto.favoritePhoto
import com.neki.photo.domain.entity.QPhotoImage.photoImage
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoImageQueryRepository
 * author         : koo
 * date           : 2026. 1. 13. 오후 10:40
 * description    : PhotoImage QueryDSL Repository for pagination
 */
@Repository
class PhotoImageQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findOwnedPhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage> =
        queryFactory.selectFrom(photoImage)
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

    fun findOwnedPhotosWithFavorite(
        userId: Long,
        folderId: Long?,
        offset: Int,
        limit: Int,
        sortOrder: SortOrder,
    ): List<PhotoWithFavorite> = queryFactory
        .select(
            Projections.constructor(
                PhotoWithFavorite::class.java,
                photoImage,
                CaseBuilder()
                    .`when`(favoritePhoto.id.photoId.isNotNull).then(true)
                    .otherwise(false),
            ),
        )
        .from(photoImage)
        .leftJoin(favoritePhoto)
        .on(
            favoritePhoto.id.userId.eq(photoImage.userId),
            favoritePhoto.id.photoId.eq(photoImage.id),
        )
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

    fun findLatestFavoritePhoto(userId: Long): PhotoImage? = queryFactory.selectFrom(photoImage)
        .innerJoin(favoritePhoto)
        .on(
            favoritePhoto.id.userId.eq(photoImage.userId),
            favoritePhoto.id.photoId.eq(photoImage.id),
        )
        .where(
            photoImage.userId.eq(userId),
        )
        .orderBy(favoritePhoto.createdAt.desc())
        .limit(1)
        .fetchOne()

    fun findOwnedPhotoWithFavorite(userId: Long, photoId: Long): PhotoWithFavorite? = queryFactory
        .select(
            Projections.constructor(
                PhotoWithFavorite::class.java,
                photoImage,
                CaseBuilder()
                    .`when`(favoritePhoto.id.photoId.isNotNull).then(true)
                    .otherwise(false),
            ),
        )
        .from(photoImage)
        .leftJoin(favoritePhoto)
        .on(
            favoritePhoto.id.userId.eq(photoImage.userId),
            favoritePhoto.id.photoId.eq(photoImage.id),
        )
        .where(
            photoImage.userId.eq(userId),
            photoImage.id.eq(photoId),
        )
        .fetchOne()

    fun updatePhotosFolderIdToNull(userId: Long, folderIds: List<Long>): Int {
        if (folderIds.isEmpty()) return 0

        return queryFactory
            .update(photoImage)
            .setNull(photoImage.folderId)
            .where(photoImage.userId.eq(userId), photoImage.folderId.`in`(folderIds), photoImage.deletedAt.isNull)
            .execute().toInt()
    }

    fun getPhotoIdsByFolderIds(userId: Long, folderIds: List<Long>): List<Long> {
        if (folderIds.isEmpty()) return emptyList()

        return queryFactory
            .select(photoImage.id)
            .from(photoImage)
            .where(
                photoImage.userId.eq(userId),
                photoImage.folderId.`in`(folderIds),
            )
            .fetch()
    }

    fun getRegisteredMediaIds(mediaIds: List<Long>): Set<Long> {
        if (mediaIds.isEmpty()) return emptySet()

        return queryFactory
            .select(photoImage.mediaId)
            .from(photoImage)
            .where(photoImage.mediaId.`in`(mediaIds))
            .fetch()
            .toSet()
    }

    fun removePhotosFromFolder(userId: Long, folderId: Long, photoIds: List<Long>): Int {
        if (photoIds.isEmpty()) return 0

        return queryFactory
            .update(photoImage)
            .setNull(photoImage.folderId)
            .where(
                photoImage.userId.eq(userId),
                photoImage.folderId.eq(folderId),
                photoImage.id.`in`(photoIds),
                photoImage.deletedAt.isNull,
            )
            .execute().toInt()
    }
}
