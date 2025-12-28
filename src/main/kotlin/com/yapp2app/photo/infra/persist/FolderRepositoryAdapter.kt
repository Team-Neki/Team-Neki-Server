package com.yapp2app.photo.infra.persist

import com.yapp2app.photo.application.port.FolderRepositoryPort
import com.yapp2app.photo.domain.entity.Folder
import com.yapp2app.photo.infra.persist.jpa.JpaFolderRepository
import org.springframework.data.repository.findByIdOrNull
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

    override fun deleteById(folderId: Long) {
        jpaRepository.deleteById(folderId)
    }

    override fun deleteAllById(folderIds: List<Long>) {
        jpaRepository.deleteAllById(folderIds)
    }

    override fun findAll(userId: Long): List<Folder> = jpaRepository.findAllByUserId(userId)

    override fun findAllByIdIn(userId: Long, folderIds: List<Long>): List<Folder> =
        jpaRepository.findAllByUserIdAndIdIn(userId, folderIds)

    override fun findById(folderId: Long): Folder? = jpaRepository.findByIdOrNull(folderId)
}
