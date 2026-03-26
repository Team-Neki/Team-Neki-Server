package com.neki.photo.infra.persist

import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.domain.entity.PhotoImageFolder
import com.neki.photo.infra.persist.jpa.JpaPhotoImageFolderRepository
import org.springframework.stereotype.Repository

@Repository
class PhotoImageFolderRepositoryAdapter(private val jpaRepository: JpaPhotoImageFolderRepository) :
    PhotoImageFolderRepositoryPort {

    override fun saveAll(photoImageIds: List<Long>, folderId: Long) {
        if (photoImageIds.isEmpty()) return
        val entities = photoImageIds.map { PhotoImageFolder(photoImageId = it, folderId = folderId) }
        jpaRepository.saveAll(entities)
    }

    override fun deleteByPhotoImageIds(photoImageIds: List<Long>) {
        if (photoImageIds.isEmpty()) return
        jpaRepository.deleteAllByPhotoImageIdIn(photoImageIds)
    }

    override fun deleteByFolderIds(folderIds: List<Long>) {
        if (folderIds.isEmpty()) return
        jpaRepository.deleteAllByFolderIdIn(folderIds)
    }

    override fun deleteByPhotoImageIdsAndFolderId(photoImageIds: List<Long>, folderId: Long) {
        if (photoImageIds.isEmpty()) return
        jpaRepository.deleteAllByPhotoImageIdInAndFolderId(photoImageIds, folderId)
    }

    override fun getPhotoImageIdsByFolderIds(folderIds: List<Long>): List<Long> {
        if (folderIds.isEmpty()) return emptyList()
        return jpaRepository.findAllByFolderIdIn(folderIds).map { it.photoImageId }
    }
}
