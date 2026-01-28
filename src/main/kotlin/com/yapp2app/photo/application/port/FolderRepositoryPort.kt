package com.yapp2app.photo.application.port

import com.yapp2app.photo.application.contract.FolderWithStats
import com.yapp2app.photo.domain.entity.Folder
import java.time.LocalDateTime

/**
 * fileName       : FolderRepositoryPort
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:58
 * description    : Folder 영속성 관련 포트 (command + query)
 */
interface FolderRepositoryPort {

    fun save(folder: Folder): Folder

    fun deleteOwnedFolders(userId: Long, folderIds: List<Long>): Int

    fun listOwnedFolders(userId: Long): List<Folder>

    fun listOwnedFoldersWithStats(userId: Long): List<FolderWithStats>

    fun getOwnedFolder(userId: Long, folderId: Long): Folder?
    fun getOwnedFolders(userId: Long, folderIds: List<Long>): List<Folder>

    fun existsOwnedFolderName(userId: Long, name: String): Boolean

    /**
     * 폴더의 커버 이미지를 조건부 업데이트
     * 새 사진이 더 최신일 때만 업데이트 (동시성 보장)
     */
    fun updateCoverPhotoIfNewer(
        userId: Long,
        folderId: Long,
        newCoverPhotoId: Long,
        newCoverPhotoCreatedAt: LocalDateTime,
    ): Int

    /**
     * 여러 폴더의 커버 이미지 재계산
     */
    fun recalculateCoverPhotos(userId: Long, folderIds: List<Long>): Int
}
