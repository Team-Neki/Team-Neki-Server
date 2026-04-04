package com.neki.photo.infra.persist.jpa

import com.neki.photo.domain.entity.PhotoImageFolder
import org.springframework.data.jpa.repository.JpaRepository

interface JpaPhotoImageFolderRepository : JpaRepository<PhotoImageFolder, Long> {

    fun deleteAllByPhotoImageIdIn(photoImageIds: List<Long>)

    fun deleteAllByFolderIdIn(folderIds: List<Long>)

    fun deleteAllByPhotoImageIdInAndFolderId(photoImageIds: List<Long>, folderId: Long)
}
