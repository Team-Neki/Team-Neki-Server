package com.yapp2app.photo.infra.persist

import com.yapp2app.photo.application.port.FolderRepositoryPort
import com.yapp2app.photo.domain.entity.Folder
import com.yapp2app.photo.infra.persist.jpa.JpaFolderRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : FolderRepositoryAdapter
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:59
 * description    : File 영속성에 대한 Adapter (command + query)
 */
@Repository
class FolderRepositoryAdapter(private val jpaRepository: JpaFolderRepository) : FolderRepositoryPort {

    override fun save(folder: Folder): Folder = jpaRepository.save(folder)

    override fun deleteOwnedFolder(userId: Long, folderId: Long): Int =
        jpaRepository.deleteByUserIdAndId(userId, folderId)

    override fun deleteOwnedFolders(userId: Long, folderIds: List<Long>): Int =
        jpaRepository.deleteAllByUserIdAndIdIn(userId, folderIds)

    override fun listOwnedFolders(userId: Long): List<Folder> = jpaRepository.findAllByUserId(userId)

    override fun getOwnedFolders(userId: Long, folderIds: List<Long>): List<Folder> =
        jpaRepository.findAllByUserIdAndIdIn(userId, folderIds)

    override fun getOwnedFolder(userId: Long, folderId: Long) = jpaRepository.findByUserIdAndId(userId, folderId)

    override fun existsOwnedFolderName(userId: Long, name: String): Boolean =
        jpaRepository.existsByUserIdAndName(userId, name)
}
