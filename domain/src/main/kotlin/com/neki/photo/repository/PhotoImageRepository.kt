package com.neki.photo.repository

import com.neki.common.domain.vo.SortOrder
import com.neki.photo.models.PhotoImage
import com.neki.photo.models.PhotoWithFavorite

/**
 * fileName       : PhotoImageRepository
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:26
 * description    : Photo image repository port
 */
interface PhotoImageRepository {

    /**
     * 저장
     */
    fun save(photoImage: PhotoImage): PhotoImage

    fun saveAll(photoImages: List<PhotoImage>): List<PhotoImage>

    fun getRegisteredMediaIds(mediaIds: List<Long>): Set<Long>

    /**
     * 조회
     */
    fun existsOwnedPhoto(userId: Long, photoId: Long): Boolean

    fun getOwnedPhoto(userId: Long, photoId: Long): PhotoImage?

    fun getOwnedPhotos(userId: Long, photoIds: List<Long>): List<PhotoImage>

    fun getLatestFavoritePhoto(userId: Long): PhotoImage?

    fun getOwnedPhotoWithFavorite(userId: Long, photoId: Long): PhotoWithFavorite?

    fun listOwnedPhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage>

    fun listOwnedPhotosWithFavorite(
        userId: Long,
        folderId: Long?,
        offset: Int,
        limit: Int,
        sortOrder: SortOrder,
    ): List<PhotoWithFavorite>

    fun listOwnedFavoritePhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage>

    fun countOwnedPhotos(userId: Long, folderId: Long?): Long

    fun countOwnedFavoritePhotos(userId: Long): Long

    /**
     * 삭제
     */
    fun deleteOwnedPhotos(userId: Long, photoIds: List<Long>): List<PhotoImage>
}
