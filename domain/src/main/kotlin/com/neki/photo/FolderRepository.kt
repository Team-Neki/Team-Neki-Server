package com.neki.photo

import com.neki.photo.models.Folder
import com.neki.photo.models.FolderStats

/**
 * fileName       : FolderRepository
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:58
 * description    : Folder 영속성 관련 포트 (command + query)
 */
interface FolderRepository {

    fun save(folder: Folder): Folder

    fun deleteOwnedFolders(userId: Long, folderIds: List<Long>): Int

    fun listOwnedFolders(userId: Long): List<Folder>

    fun listOwnedFoldersWithStats(userId: Long, limit: Int?): List<FolderStats>

    fun getOwnedFolder(userId: Long, folderId: Long): Folder?
    fun getOwnedFolders(userId: Long, folderIds: List<Long>): List<Folder>

    fun existsOwnedFolderName(userId: Long, name: String): Boolean
}
