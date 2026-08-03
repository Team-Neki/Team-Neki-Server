package com.neki.photo.infra.persist.jpa

import com.neki.photo.models.PhotoImageFolder
import org.springframework.data.jpa.repository.JpaRepository

interface JpaPhotoImageFolderRepository : JpaRepository<PhotoImageFolder, Long> {

    fun deleteAllByPhotoImageIdIn(photoImageIds: List<Long>)

    fun deleteAllByFolderIdIn(folderIds: List<Long>)

    fun deleteAllByPhotoImageIdInAndFolderId(photoImageIds: List<Long>, folderId: Long)

    fun findAllByPhotoImageIdInAndFolderIdIn(photoImageIds: List<Long>, folderIds: List<Long>): List<PhotoImageFolder>

    fun findAllByFolderIdIn(folderIds: List<Long>): List<PhotoImageFolder>
}
