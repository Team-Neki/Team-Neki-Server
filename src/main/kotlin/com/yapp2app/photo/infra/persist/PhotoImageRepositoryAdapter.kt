package com.yapp2app.photo.infra.persist

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.photo.application.contract.PhotoWithFavorite
import com.yapp2app.photo.application.port.PhotoImageRepositoryPort
import com.yapp2app.photo.domain.entity.PhotoImage
import com.yapp2app.photo.infra.persist.jpa.JpaPhotoImageRepository
import com.yapp2app.photo.infra.persist.jpa.PhotoImageQueryRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoImageRepositoryAdapter
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:25
 * description    : Photo image Repository Adapter
 */
@Repository
class PhotoImageRepositoryAdapter(
    private val jpaRepository: JpaPhotoImageRepository,
    private val queryRepository: PhotoImageQueryRepository,
) : PhotoImageRepositoryPort {

    override fun getOwnedPhotoWithFavorite(userId: Long, photoId: Long): PhotoWithFavorite? =
        queryRepository.findOwnedPhotoWithFavorite(userId, photoId)

    override fun save(photoImage: PhotoImage): PhotoImage = jpaRepository.save(photoImage)
    override fun saveAll(photoImages: List<PhotoImage>): List<PhotoImage> = jpaRepository.saveAll(photoImages)

    override fun listOwnedPhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage> =
        queryRepository.findOwnedPhotos(userId, offset, limit, sortOrder)

    override fun listOwnedPhotosWithFavorite(
        userId: Long,
        offset: Int,
        limit: Int,
        sortOrder: SortOrder,
    ): List<PhotoWithFavorite> = queryRepository.findOwnedPhotosWithFavorite(userId, offset, limit, sortOrder)

    override fun listOwnedFavoritePhotos(
        userId: Long,
        offset: Int,
        limit: Int,
        sortOrder: SortOrder,
    ): List<PhotoImage> = queryRepository.findOwnedFavoritePhotos(userId, offset, limit, sortOrder)

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

    override fun existsOwnedPhoto(userId: Long, photoId: Long): Boolean =
        jpaRepository.existsByUserIdAndId(userId, photoId)

    override fun getLatestOwnedPhoto(userId: Long): PhotoImage? = queryRepository.findLatestOwnedPhoto(userId)

    override fun updatePhotosFolderIdToNull(userId: Long, folderIds: List<Long>): Int =
        queryRepository.updatePhotosFolderIdToNull(userId, folderIds)

    override fun getAffectedFolderIds(userId: Long, photoIds: List<Long>): List<Long> =
        queryRepository.getAffectedFolderIds(userId, photoIds)
}
