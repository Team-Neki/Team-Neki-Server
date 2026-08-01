package com.neki.photo.application.port

import com.neki.photo.application.port.dto.PhotoContract
import com.neki.photo.entity.Folder

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

    fun listOwnedFoldersWithStats(userId: Long, limit: Int?): List<PhotoContract.FolderWithStats>

    fun getOwnedFolder(userId: Long, folderId: Long): Folder?
    fun getOwnedFolders(userId: Long, folderIds: List<Long>): List<Folder>

    fun existsOwnedFolderName(userId: Long, name: String): Boolean
}
