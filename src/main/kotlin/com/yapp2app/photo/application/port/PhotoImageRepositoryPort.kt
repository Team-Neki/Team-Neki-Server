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

    fun getOwnedPhotoWithFavorite(userId: Long, photoId: Long): PhotoWithFavorite?

    fun save(photoImage: PhotoImage): PhotoImage
    fun saveAll(photoImages: List<PhotoImage>): List<PhotoImage>

    fun listOwnedPhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage>

    fun listOwnedPhotosWithFavorite(
        userId: Long,
        folderId: Long?,
        offset: Int,
        limit: Int,
        sortOrder: SortOrder,
    ): List<PhotoWithFavorite>

    fun listOwnedFavoritePhotos(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<PhotoImage>

    fun deleteOwnedPhotos(userId: Long, photoIds: List<Long>): List<PhotoImage>

    fun getOwnedPhoto(userId: Long, photoId: Long): PhotoImage?

    fun existsOwnedPhoto(userId: Long, photoId: Long): Boolean

    fun getLatestOwnedPhoto(userId: Long): PhotoImage?

    fun updatePhotosFolderIdToNull(userId: Long, folderIds: List<Long>): Int

    /**
     * 삭제 예정인 사진들이 속한 폴더 ID 조회
     */
    fun getAffectedFolderIds(userId: Long, photoIds: List<Long>): List<Long>
}
