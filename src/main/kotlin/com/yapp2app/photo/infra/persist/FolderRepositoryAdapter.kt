package com.yapp2app.photo.infra.persist

import com.yapp2app.photo.application.contract.FolderWithStats
import com.yapp2app.photo.application.port.FolderRepositoryPort
import com.yapp2app.photo.domain.entity.Folder
import com.yapp2app.photo.infra.persist.jpa.FolderQueryRepository
import com.yapp2app.photo.infra.persist.jpa.JpaFolderRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * fileName       : FolderRepositoryAdapter
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:59
 * description    : File 영속성에 대한 Adapter (command + query)
 */
@Repository
class FolderRepositoryAdapter(
    private val jpaRepository: JpaFolderRepository,
    private val queryRepository: FolderQueryRepository,
) : FolderRepositoryPort {

    override fun save(folder: Folder): Folder = jpaRepository.save(folder)

    override fun deleteOwnedFolders(userId: Long, folderIds: List<Long>): Int =
        queryRepository.deleteOwnedFolders(userId, folderIds)

    override fun listOwnedFolders(userId: Long): List<Folder> = jpaRepository.findAllByUserId(userId)

    override fun listOwnedFoldersWithStats(userId: Long): List<FolderWithStats> =
        queryRepository.findOwnedFoldersWithStats(userId)

    override fun getOwnedFolders(userId: Long, folderIds: List<Long>): List<Folder> =
        jpaRepository.findAllByUserIdAndIdIn(userId, folderIds)

    override fun getOwnedFolder(userId: Long, folderId: Long) = jpaRepository.findByUserIdAndId(userId, folderId)

    override fun existsOwnedFolderName(userId: Long, name: String): Boolean =
        jpaRepository.existsByUserIdAndName(userId, name)

    override fun updateCoverPhotoIfNewer(
        userId: Long,
        folderId: Long,
        newCoverPhotoId: Long,
        newCoverPhotoCreatedAt: LocalDateTime,
    ): Int = queryRepository.updateCoverPhotoIfNewer(userId, folderId, newCoverPhotoId, newCoverPhotoCreatedAt)

    override fun recalculateCoverPhotos(userId: Long, folderIds: List<Long>): Int =
        queryRepository.recalculateCoverPhotos(userId, folderIds)
}
