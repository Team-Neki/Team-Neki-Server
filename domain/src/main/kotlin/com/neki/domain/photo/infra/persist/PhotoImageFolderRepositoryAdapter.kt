package com.neki.domain.photo.infra.persist

import com.neki.domain.photo.infra.persist.jpa.JpaPhotoImageFolderRepository
import com.neki.domain.photo.models.PhotoImageFolder
import com.neki.domain.photo.repository.PhotoImageFolderRepository
import org.springframework.stereotype.Repository

@Repository
class PhotoImageFolderRepositoryAdapter(private val jpaRepository: JpaPhotoImageFolderRepository) :
    PhotoImageFolderRepository {

    override fun saveAll(photoImageIds: List<Long>, folderId: Long) {
        if (photoImageIds.isEmpty()) return
        val entities = photoImageIds.map { PhotoImageFolder(photoImageId = it, folderId = folderId) }
        jpaRepository.saveAll(entities)
    }

    override fun saveAll(mappings: List<Pair<Long, Long>>) {
        if (mappings.isEmpty()) return
        val entities: List<PhotoImageFolder> = mappings.map { (photoImageId, folderId) ->
            PhotoImageFolder(photoImageId = photoImageId, folderId = folderId)
        }
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

    override fun findByPhotoImageIdsAndFolderIds(
        photoImageIds: List<Long>,
        folderIds: List<Long>,
    ): List<PhotoImageFolder> {
        if (photoImageIds.isEmpty()) return emptyList()
        return jpaRepository.findAllByPhotoImageIdInAndFolderIdIn(photoImageIds, folderIds)
    }

    override fun getPhotoImageIdsByFolderIds(folderIds: List<Long>): List<Long> {
        if (folderIds.isEmpty()) return emptyList()
        return jpaRepository.findAllByFolderIdIn(folderIds).map { it.photoImageId }
    }
}
