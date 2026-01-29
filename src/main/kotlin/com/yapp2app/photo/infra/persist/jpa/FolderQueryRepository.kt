package com.yapp2app.photo.infra.persist.jpa

import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.media.domain.entity.QMedia.media
import com.yapp2app.photo.application.contract.FolderWithStats
import com.yapp2app.photo.domain.entity.QFolder.folder
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

    fun findOwnedFoldersWithStats(userId: Long): List<FolderWithStats> = queryFactory
        .select(
            Projections.constructor(
                FolderWithStats::class.java,
                folder.id,
                folder.name,
                JPAExpressions
                    .select(media.storageKey)
                    .from(media)
                    .where(media.id.eq(photoImage.mediaId.max())),
                photoImage.id.count(),
            ),
        )
        .from(folder)
        .leftJoin(photoImage).on(
            photoImage.folderId.eq(folder.id),
            photoImage.userId.eq(userId),
        )
        .where(folder.userId.eq(userId))
        .groupBy(folder.id)
        .fetch()
}
