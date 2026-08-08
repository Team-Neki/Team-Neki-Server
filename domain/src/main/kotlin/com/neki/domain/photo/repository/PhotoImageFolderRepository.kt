package com.neki.domain.photo.repository

import com.neki.domain.photo.models.PhotoImageFolder

interface PhotoImageFolderRepository {

    fun saveAll(photoImageIds: List<Long>, folderId: Long)

    fun saveAll(mappings: List<Pair<Long, Long>>)

    fun deleteByPhotoImageIds(photoImageIds: List<Long>)

    fun deleteByFolderIds(folderIds: List<Long>)

    fun deleteByPhotoImageIdsAndFolderId(photoImageIds: List<Long>, folderId: Long)

    fun findByPhotoImageIdsAndFolderIds(photoImageIds: List<Long>, folderIds: List<Long>): List<PhotoImageFolder>

    fun getPhotoImageIdsByFolderIds(folderIds: List<Long>): List<Long>
}
