package com.yapp2app.photo.infra.persist.jpa

import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.photo.domain.entity.QFolder.folder
import org.springframework.stereotype.Repository

/**
 * fileName       : FolderQueryRepository
 * author         : koo
 * date           : 2026. 1. 16. 오후 10:48
 * description    : Folder Querydsl 구현체
 */
@Repository
class FolderQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun deleteOwnedFolder(userId: Long, folderId: Long): Int = queryFactory
        .delete(folder)
        .where(folder.userId.eq(userId), folder.id.eq(folderId))
        .execute().toInt()

    fun deleteOwnedFolders(userId: Long, folderIds: List<Long>): Int = queryFactory
        .delete(folder)
        .where(folder.userId.eq(userId), folder.id.`in`(folderIds))
        .execute().toInt()
}
