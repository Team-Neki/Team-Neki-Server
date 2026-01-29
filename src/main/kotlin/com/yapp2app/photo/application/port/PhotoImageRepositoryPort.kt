package com.yapp2app.photo.application.port

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.photo.application.contract.PhotoWithFavorite
import com.yapp2app.photo.domain.entity.PhotoImage

/**
 * fileName       : PhotoImageRepositoryPort
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:26
 * description    : Photo image repository port
 */
interface PhotoImageRepositoryPort {

    /**
     * 저장
     */
    fun save(photoImage: PhotoImage): PhotoImage

    fun saveAll(photoImages: List<PhotoImage>): List<PhotoImage>

    /**
     * 조회
     */
    fun existsOwnedPhoto(userId: Long, photoId: Long): Boolean

    fun getOwnedPhoto(userId: Long, photoId: Long): PhotoImage?

    fun getLatestOwnedPhoto(userId: Long): PhotoImage?

    fun getOwnedPhotoWithFavorite(userId: Long, photoId: Long): PhotoWithFavorite?

    fun getPhotoIdsByFolderIds(userId: Long, folderIds: List<Long>): List<Long>

    fun listOwnedPhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage>

    fun listOwnedPhotosWithFavorite(
        userId: Long,
        folderId: Long?,
        offset: Int,
        limit: Int,
        sortOrder: SortOrder,
    ): List<PhotoWithFavorite>

    fun listOwnedFavoritePhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage>

    /**
     * 삭제
     */
    fun deleteOwnedPhotos(userId: Long, photoIds: List<Long>): List<PhotoImage>

    fun removePhotosFromFolder(userId: Long, folderId: Long, photoIds: List<Long>): Int

    /**
     * 갱신
     */
    fun updatePhotosFolderIdToNull(userId: Long, folderIds: List<Long>): Int
}
