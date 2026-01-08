package com.yapp2app.photo.infra.persist

import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
import com.yapp2app.photo.domain.entity.PhotoImage
import com.yapp2app.photo.infra.persist.jpa.JpaPhotoImageRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoImageRepositoryAdapter
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:25
 * description    : Photo image Repository Adapter
 */
@Repository
class PhotoImageRepositoryAdapter(private val jpaRepository: JpaPhotoImageRepository) : PhotoImageRepositoryPort {

    override fun save(photoImage: PhotoImage): PhotoImage = jpaRepository.save(photoImage)

    override fun listOwnedPhotos(userId: Long, folderId: Long?): List<PhotoImage> {
        TODO("Not yet implemented")
    }

    override fun deleteOwnedPhoto(userId: Long, photoId: Long): PhotoImage? {
        val photo = jpaRepository.findByUserIdAndId(userId, photoId)
            ?: return null

        jpaRepository.delete(photo)

        return photo
    }

    override fun deleteOwnedPhotos(userId: Long, photoIds: List<Long>): List<PhotoImage> {
        val photos = jpaRepository.findAllByUserIdAndIdIn(userId, photoIds)

        if (photos.isEmpty()) {
            return emptyList()
        }

        jpaRepository.deleteAll(photos)

        return photos
    }

    override fun getOwnedPhoto(userId: Long, photoId: Long): PhotoImage? =
        jpaRepository.findByUserIdAndId(userId, photoId)
}
