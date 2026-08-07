package com.neki.photo.service

import com.neki.common.code.ResultCode
import com.neki.common.domain.vo.Page
import com.neki.common.exception.BusinessException
import com.neki.photo.dto.FolderCommand
import com.neki.photo.dto.PhotoImageCommand
import com.neki.photo.dto.PhotoImageQuery
import com.neki.photo.dto.UserScoped
import com.neki.photo.models.PhotoImage
import com.neki.photo.models.PhotoWithFavorite
import com.neki.photo.repository.PhotoImageRepository
import org.springframework.stereotype.Component

/**
 * fileName       : PhotoService
 * author         : koo
 * date           : 2026. 8. 3. 오전 1:43
 * description    : 사진 도메인 서비스. 사진 애그리거트의 불변식과 상태 전이만 다룬다.
 */
@Component
class PhotoService(private val photoImageRepository: PhotoImageRepository) {

    fun getOwnedPhotoWithFavorite(query: PhotoImageQuery.GetPhoto): PhotoWithFavorite =
        photoImageRepository.getOwnedPhotoWithFavorite(query.userId, query.photoId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

    fun listPhotosWithFavorite(query: PhotoImageQuery.GetPhotos): Page<PhotoWithFavorite> = query.pagination.slice(
        photoImageRepository.listOwnedPhotosWithFavorite(
            userId = query.userId,
            folderId = query.folderId,
            offset = query.pagination.offset,
            limit = query.pagination.limit,
            sortOrder = query.pagination.sortOrder,
        ),
    )

    fun countPhotos(query: PhotoImageQuery.GetPhotos): Long =
        photoImageRepository.countOwnedPhotos(userId = query.userId, folderId = query.folderId)

    fun listFavoritePhotos(query: PhotoImageQuery.GetFavoritePhotos): Page<PhotoImage> = query.pagination.slice(
        photoImageRepository.listOwnedFavoritePhotos(
            userId = query.userId,
            offset = query.pagination.offset,
            limit = query.pagination.limit,
            sortOrder = query.pagination.sortOrder,
        ),
    )

    fun countFavoritePhotos(query: PhotoImageQuery.GetFavoritePhotos): Long =
        photoImageRepository.countOwnedFavoritePhotos(query.userId)

    fun getLatestFavoritePhoto(query: PhotoImageQuery.GetFavoriteSummary): PhotoImage? =
        photoImageRepository.getLatestFavoritePhoto(query.userId)

    fun putPhoto(command: PhotoImageCommand.PutPhoto) {
        val photo: PhotoImage = getOwnedPhoto(command.userId, command.photoId)
        photo.update(command.memo, command.capturedAt)
    }

    fun updatePhotoMemo(command: PhotoImageCommand.UpdatePhoto) {
        val photo: PhotoImage = getOwnedPhoto(command.userId, command.photoId)
        command.memo?.let { photo.updateMemo(it) }
    }

    /**
     * 해당 사진이 사용자 소유인지 확인한다.
     */
    fun validatePhotoOwned(command: PhotoImageCommand.UpdatePhotoFavorite) {
        if (!photoImageRepository.existsOwnedPhoto(command.userId, command.photoId)) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }
    }

    /**
     * 요청한 사진이 모두 해당 사용자 소유인지 확인한다.
     */
    fun validatePhotosOwned(command: FolderCommand.PhotosToTargetFolders) {
        val ownedPhotos: List<PhotoImage> = photoImageRepository.getOwnedPhotos(command.userId, command.photoIds)

        if (command.photoIds.size != ownedPhotos.size) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }
    }

    /**
     * 한 번의 업로드에 같은 media를 두 번 담을 수 없다.
     */
    fun validateNoDuplicateMediaIds(command: PhotoImageCommand.UploadPhoto) {
        val duplicates: Set<Long> = command.uploads.map { it.mediaId }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }

    /**
     * 이미 등록된 media는 제외하고 새로 저장할 사진만 만든다 (재요청 멱등성).
     */
    fun createNewPhotos(command: PhotoImageCommand.UploadPhoto): List<PhotoImage> {
        val mediaIds: List<Long> = command.uploads.map { it.mediaId }
        val existingMediaIds: Set<Long> = photoImageRepository.getRegisteredMediaIds(mediaIds)

        return command.uploads
            .filter { it.mediaId !in existingMediaIds }
            .map { upload ->
                PhotoImage(
                    userId = command.userId,
                    mediaId = upload.mediaId,
                    memo = upload.memo,
                    uploadMethod = upload.uploadMethod,
                    capturedAt = upload.capturedAt,
                )
            }
    }

    fun savePhotos(photos: List<PhotoImage>): List<PhotoImage> = photoImageRepository.saveAll(photos)

    fun deletePhotos(command: PhotoImageCommand.DeletePhotos): List<PhotoImage> =
        photoImageRepository.deleteOwnedPhotos(command.userId, command.photoIds)

    /**
     * 오케스트레이션 중에 정해지는 사진 목록을 지운다 (command에 담기지 않는 값).
     */
    fun deleteOwnedPhotos(request: UserScoped, photoIds: List<Long>): List<PhotoImage> =
        photoImageRepository.deleteOwnedPhotos(request.userId, photoIds)

    private fun getOwnedPhoto(userId: Long, photoId: Long): PhotoImage =
        photoImageRepository.getOwnedPhoto(userId, photoId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)
}
