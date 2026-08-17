package com.neki.domain.photo.infra.persist

import com.neki.domain.photo.infra.persist.jpa.FolderQueryRepository
import com.neki.domain.photo.infra.persist.jpa.JpaFolderRepository
import com.neki.domain.photo.models.Folder
import com.neki.domain.photo.models.FolderStats
import com.neki.domain.photo.repository.FolderRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : FolderRepositoryAdapter
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:59
 * description    : File 영속성에 대한 Adapter (query + query)
 */
@Repository
class FolderRepositoryAdapter(
    private val jpaRepository: JpaFolderRepository,
    private val queryRepository: FolderQueryRepository,
) : FolderRepository {

    override fun save(folder: Folder): Folder = jpaRepository.save(folder)

    override fun deleteOwnedFolders(userId: Long, folderIds: List<Long>): Int =
        queryRepository.deleteOwnedFolders(userId, folderIds)

    override fun listOwnedFolders(userId: Long): List<Folder> = jpaRepository.findAllByUserId(userId)

    override fun listOwnedFoldersWithStats(userId: Long, limit: Int?): List<FolderStats> =
        queryRepository.findOwnedFoldersWithStats(userId, limit)

    override fun getOwnedFolders(userId: Long, folderIds: List<Long>): List<Folder> =
        jpaRepository.findAllByUserIdAndIdIn(userId, folderIds)

    override fun getOwnedFolder(userId: Long, folderId: Long) = jpaRepository.findByUserIdAndId(userId, folderId)

    override fun existsOwnedFolderName(userId: Long, name: String): Boolean =
        jpaRepository.existsByUserIdAndName(userId, name)
}
