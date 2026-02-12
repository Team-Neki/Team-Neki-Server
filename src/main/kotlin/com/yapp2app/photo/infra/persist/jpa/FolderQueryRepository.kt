package com.yapp2app.photo.infra.persist.jpa

import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.media.domain.entity.QMedia.media
import com.yapp2app.photo.application.contract.FolderWithStats
import com.yapp2app.photo.domain.entity.QFolder.folder
import com.yapp2app.photo.domain.entity.QPhotoImage
import com.yapp2app.photo.domain.entity.QPhotoImage.photoImage
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
        val folderStats = queryFactory
            .select(folder.id, folder.name, photoImage.id.count())
            .from(folder)
            .leftJoin(photoImage).on(
                photoImage.folderId.eq(folder.id),
                photoImage.userId.eq(userId),
            )
            .where(folder.userId.eq(userId))
            .groupBy(folder.id, folder.name)
            .apply { limit?.let { limit(it.toLong()) } }
            .fetch()

        if (folderStats.isEmpty()) return emptyList()

        val folderIdsWithPhotos = folderStats
            .filter { it.get(photoImage.id.count())!! > 0L }
            .mapNotNull { it.get(folder.id) }

        val coverMap = if (folderIdsWithPhotos.isNotEmpty()) {
            val latestPhoto = QPhotoImage("latestPhoto")
            val maxIdSubquery = QPhotoImage("maxIdSubquery")

            queryFactory
                .select(latestPhoto.folderId, media.storageKey)
                .from(latestPhoto)
                .join(media).on(media.id.eq(latestPhoto.mediaId))
                .where(
                    latestPhoto.userId.eq(userId),
                    latestPhoto.folderId.`in`(folderIdsWithPhotos),
                    latestPhoto.id.eq(
                        JPAExpressions
                            .select(maxIdSubquery.id.max())
                            .from(maxIdSubquery)
                            .where(
                                maxIdSubquery.folderId.eq(latestPhoto.folderId),
                                maxIdSubquery.userId.eq(userId),
                            ),
                    ),
                )
                .fetch()
                .associate { it.get(latestPhoto.folderId)!! to it.get(media.storageKey)!! }
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
