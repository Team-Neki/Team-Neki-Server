package com.yapp2app.photo.infra.persist.jpa

import com.yapp2app.photo.domain.entity.Folder
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Folder f where f.userId = :userId and f.id = :folderId")
    fun deleteByUserIdAndId(@Param("userId") userId: Long, @Param("folderId") folderId: Long): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Folder f where f.userId = :userId and f.id in :folderIds")
    fun deleteAllByUserIdAndIdIn(userId: Long, folderIds: List<Long>): Int
}
