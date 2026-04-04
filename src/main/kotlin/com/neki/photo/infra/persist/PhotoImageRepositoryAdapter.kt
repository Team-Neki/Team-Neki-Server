package com.neki.photo.infra.persist

import com.neki.common.api.dto.ResultCode
import com.neki.common.domain.vo.SortOrder
import com.neki.common.exception.BusinessException
import com.neki.photo.application.contract.PhotoWithFavorite
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.domain.entity.PhotoImage
import com.neki.photo.infra.persist.jpa.JpaPhotoImageRepository
import com.neki.photo.infra.persist.jpa.PhotoImageQueryRepository
import org.springframework.dao.DataIntegrityViolationException
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
    private val photoImageFolderRepository: PhotoImageFolderRepositoryPort,
) : PhotoImageRepositoryPort {

    override fun getOwnedPhotoWithFavorite(userId: Long, photoId: Long): PhotoWithFavorite? =
        queryRepository.findOwnedPhotoWithFavorite(userId, photoId)

    override fun save(photoImage: PhotoImage): PhotoImage = jpaRepository.save(photoImage)
    override fun saveAll(photoImages: List<PhotoImage>): List<PhotoImage> = try {
        jpaRepository.saveAll(photoImages)
    } catch (e: DataIntegrityViolationException) {
        throw BusinessException(ResultCode.ALREADY_REQUEST)
    }

    override fun getRegisteredMediaIds(mediaIds: List<Long>): Set<Long> =
        queryRepository.getRegisteredMediaIds(mediaIds)

    override fun listOwnedPhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage> =
        queryRepository.findOwnedPhotos(userId, offset, limit, sortOrder)

    override fun listOwnedPhotosWithFavorite(
        userId: Long,
        folderId: Long?,
        offset: Int,
        limit: Int,
        sortOrder: SortOrder,
    ): List<PhotoWithFavorite> = queryRepository.findOwnedPhotosWithFavorite(userId, folderId, offset, limit, sortOrder)

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

        photos.forEach { it.softDelete() }
        jpaRepository.saveAll(photos)
        jpaRepository.flush()

        // 중간 테이블에서도 연관 삭제 (dual-write)
        photoImageFolderRepository.deleteByPhotoImageIds(photoIds)

        return photos
    }

    override fun getOwnedPhoto(userId: Long, photoId: Long): PhotoImage? =
        jpaRepository.findByUserIdAndId(userId, photoId)

    override fun existsOwnedPhoto(userId: Long, photoId: Long): Boolean =
        jpaRepository.existsByUserIdAndId(userId, photoId)

    override fun countOwnedPhotos(userId: Long, photoIds: List<Long>): Int =
        jpaRepository.findAllByUserIdAndIdIn(userId, photoIds).size

    override fun getLatestFavoritePhoto(userId: Long): PhotoImage? = queryRepository.findLatestFavoritePhoto(userId)
}
