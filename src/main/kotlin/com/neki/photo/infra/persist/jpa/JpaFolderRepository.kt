package com.neki.photo.infra.persist.jpa

import com.neki.photo.domain.entity.Folder
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaFolderRepository
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:59
 * description    : File에 대한 Jpa interface
 */
interface JpaFolderRepository : JpaRepository<Folder, Long> {

    fun findByUserIdAndId(userId: Long, folderId: Long): Folder?

    fun findAllByUserId(userId: Long): List<Folder>

    fun findAllByUserIdAndIdIn(userId: Long, folderIds: List<Long>): List<Folder>

    fun existsByUserIdAndName(userId: Long, name: String): Boolean
}
