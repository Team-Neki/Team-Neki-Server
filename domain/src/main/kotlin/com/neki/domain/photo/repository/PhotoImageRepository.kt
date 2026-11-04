package com.neki.domain.photo.repository

import com.neki.core.domain.vo.SortOrder
import com.neki.domain.photo.models.PhotoImage
import com.neki.domain.photo.models.PhotoWithFavorite
import java.time.LocalDateTime

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

    /** userIds 중 [start, endExclusive) 에 올린 사진이 있는 user_id. 삭제된 사진은 @SQLRestriction 이 뺀다 */
    fun findUserIdsUploadedBetween(
        userIds: Collection<Long>,
        start: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Set<Long>

    /** userIds 마다 삭제되지 않은 사진의 마지막 업로드 시각. 사진이 없는 유저는 키가 없다 */
    fun findLastUploadedAtByUserIds(userIds: Collection<Long>): Map<Long, LocalDateTime>

    /**
     * 삭제
     */
    fun deleteOwnedPhotos(userId: Long, photoIds: List<Long>): List<PhotoImage>
}
