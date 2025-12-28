package com.yapp2app.photo.application.port

import com.yapp2app.photo.domain.entity.Folder

/**
 * fileName       : FolderRepositoryPort
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:58
 * description    : Folder 영속성 관련 포트 (command + query)
 */
interface FolderRepositoryPort {
    fun save(folder: Folder): Folder

    fun deleteById(folderId: Long)

    fun deleteAllById(folderIds: List<Long>)

    fun findAll(userId: Long): List<Folder>

    fun findAllByIdIn(userId: Long, folderIds: List<Long>): List<Folder>

    fun findById(folderId: Long): Folder?
}
