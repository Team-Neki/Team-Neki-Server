package com.neki.photo.infra.persist.jpa

import com.neki.media.entity.QMedia.media
import com.neki.photo.application.contract.FolderWithStats
import com.neki.photo.entity.QFolder.folder
import com.neki.photo.entity.QPhotoImage
import com.neki.photo.entity.QPhotoImage.photoImage
import com.neki.photo.entity.QPhotoImageFolder
import com.neki.photo.entity.QPhotoImageFolder.photoImageFolder
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : FolderQueryRepository
 * author         : koo
 * date           : 2026. 1. 16. 오후 10:48
 * description    : Folder Querydsl 구현체
 */
@Repository
class FolderQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun deleteOwnedFolders(userId: Long, folderIds: List<Long>): Int {
        if (folderIds.isEmpty()) return 0

        return queryFactory
            .delete(folder)
            .where(folder.userId.eq(userId), folder.id.`in`(folderIds))
            .execute().toInt()
    }

    fun findOwnedFoldersWithStats(userId: Long, limit: Int?): List<FolderWithStats> {
        val latestPhotoDate = photoImage.createdAt.max()

        val folderStats = queryFactory
            .select(folder.id, folder.name, photoImage.id.count(), latestPhotoDate)
            .from(folder)
            .leftJoin(photoImageFolder).on(photoImageFolder.folderId.eq(folder.id))
            .leftJoin(photoImage).on(
                photoImage.id.eq(photoImageFolder.photoImageId),
                photoImage.userId.eq(userId),
            )
            .where(folder.userId.eq(userId))
            .groupBy(folder.id, folder.name)
            .orderBy(latestPhotoDate.desc().nullsLast(), folder.createdAt.desc())
            .apply { limit?.let { limit(it.toLong()) } }
            .fetch()

        if (folderStats.isEmpty()) return emptyList()

        val folderIdsWithPhotos = folderStats
            .filter { it.get(photoImage.id.count())!! > 0L }
            .mapNotNull { it.get(folder.id) }

        val coverMap = if (folderIdsWithPhotos.isNotEmpty()) {
            val latestPhoto = QPhotoImage("latestPhoto")
            val maxIdSubquery = QPhotoImage("maxIdSubquery")
            val latestPhotoFolder = QPhotoImageFolder("latestPhotoFolder")
            val maxIdPhotoFolder = QPhotoImageFolder("maxIdPhotoFolder")

            queryFactory
                .select(latestPhotoFolder.folderId, media.storageKey)
                .from(latestPhoto)
                .join(latestPhotoFolder).on(latestPhotoFolder.photoImageId.eq(latestPhoto.id))
                .join(media).on(media.id.eq(latestPhoto.mediaId))
                .where(
                    latestPhoto.userId.eq(userId),
                    latestPhotoFolder.folderId.`in`(folderIdsWithPhotos),
                    latestPhoto.id.eq(
                        JPAExpressions
                            .select(maxIdSubquery.id.max())
                            .from(maxIdSubquery)
                            .join(maxIdPhotoFolder).on(maxIdPhotoFolder.photoImageId.eq(maxIdSubquery.id))
                            .where(
                                maxIdPhotoFolder.folderId.eq(latestPhotoFolder.folderId),
                                maxIdSubquery.userId.eq(userId),
                            ),
                    ),
                )
                .fetch()
                .associate { it.get(latestPhotoFolder.folderId)!! to it.get(media.storageKey)!! }
        } else {
            emptyMap()
        }

        return folderStats.map { tuple ->
            val folderId = tuple.get(folder.id)!!
            FolderWithStats(
                folderId = folderId,
                name = tuple.get(folder.name)!!,
                coverImageStorageKey = coverMap[folderId],
                photoCount = tuple.get(photoImage.id.count())!!,
            )
        }
    }
}
