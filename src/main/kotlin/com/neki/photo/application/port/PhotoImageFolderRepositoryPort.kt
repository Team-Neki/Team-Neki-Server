package com.neki.photo.application.port

interface PhotoImageFolderRepositoryPort {

    fun saveAll(photoImageIds: List<Long>, folderId: Long)

    fun deleteByPhotoImageIds(photoImageIds: List<Long>)

    fun deleteByFolderIds(folderIds: List<Long>)

    fun deleteByPhotoImageIdsAndFolderId(photoImageIds: List<Long>, folderId: Long)
}
