package com.neki.domain.photo.dto

/**
 * fileName       : FolderCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Folder usecase 관련 command
 */
object FolderCommand {

    /**
     * 사진을 여러 대상 폴더에 담는 요청이 공통으로 갖는 정보.
     */
    interface PhotosToTargetFolders : UserScoped {
        val photoIds: List<Long>
        val targetFolderIds: List<Long>
    }

    data class CreateFolder(override val userId: Long, val name: String) : UserScoped

    data class DeleteFolders(override val userId: Long, val folderIds: List<Long>, val deletePhotos: Boolean = false) :
        UserScoped

    data class UpdateFolder(override val userId: Long, val folderId: Long, val newName: String) : UserScoped

    data class RemovePhotosFromFolder(override val userId: Long, val folderId: Long, val photoIds: List<Long>) :
        UserScoped

    data class MovePhotosToFolder(
        override val userId: Long,
        val sourceFolderId: Long,
        override val photoIds: List<Long>,
        override val targetFolderIds: List<Long>,
    ) : PhotosToTargetFolders

    data class CopyPhotosToFolder(
        override val userId: Long,
        override val photoIds: List<Long>,
        override val targetFolderIds: List<Long>,
    ) : PhotosToTargetFolders
}
