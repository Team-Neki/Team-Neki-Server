package com.neki.photo.service

import com.neki.photo.FavoriteImageRepository
import com.neki.photo.dto.PhotoImageCommand
import com.neki.photo.dto.UserScoped
import com.neki.photo.models.FavoritePhoto
import org.springframework.stereotype.Component

/**
 * fileName       : FavoriteService
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 즐겨찾기 도메인 서비스
 */
@Component
class FavoriteService(private val favoriteImageRepository: FavoriteImageRepository) {

    fun count(request: UserScoped): Long = favoriteImageRepository.countByUserId(request.userId)

    fun add(command: PhotoImageCommand.UpdatePhotoFavorite) =
        favoriteImageRepository.add(FavoritePhoto(command.userId, command.photoId))

    fun remove(command: PhotoImageCommand.UpdatePhotoFavorite) =
        favoriteImageRepository.delete(FavoritePhoto(command.userId, command.photoId))

    fun removeAll(command: PhotoImageCommand.DeletePhotos) =
        favoriteImageRepository.deleteAll(command.userId, command.photoIds)

    /**
     * 오케스트레이션 중에 정해지는 사진 목록에 대해 처리한다 (command에 담기지 않는 값).
     */
    fun addAll(request: UserScoped, photoIds: List<Long>) = favoriteImageRepository.addAll(request.userId, photoIds)

    fun removeAll(request: UserScoped, photoIds: List<Long>) =
        favoriteImageRepository.deleteAll(request.userId, photoIds)
}
