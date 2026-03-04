package com.neki.photo.infra.persist

import com.neki.photo.application.contract.FolderWithStats
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.domain.entity.Folder
import com.neki.photo.infra.persist.jpa.FolderQueryRepository
import com.neki.photo.infra.persist.jpa.JpaFolderRepository
import org.springframework.stereotype.Repository

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

    override fun listOwnedFoldersWithStats(userId: Long, limit: Int?): List<FolderWithStats> =
        queryRepository.findOwnedFoldersWithStats(userId, limit)

    override fun getOwnedFolders(userId: Long, folderIds: List<Long>): List<Folder> =
        jpaRepository.findAllByUserIdAndIdIn(userId, folderIds)

    override fun getOwnedFolder(userId: Long, folderId: Long) = jpaRepository.findByUserIdAndId(userId, folderId)

    override fun existsOwnedFolderName(userId: Long, name: String): Boolean =
        jpaRepository.existsByUserIdAndName(userId, name)
}
