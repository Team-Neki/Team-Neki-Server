package com.yapp2app.photo.application.port

import com.yapp2app.photo.domain.entity.PhotoImage

/**
 * fileName       : PhotoImageRepositoryPort
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:26
 * description    : Photo image repository port
 */
interface PhotoImageRepositoryPort {

    fun save(photoImage: PhotoImage): PhotoImage

    fun listOwnedPhotos(userId: Long, folderId: Long?, offset: Int, limit: Int): List<PhotoImage>

    fun deleteOwnedPhoto(userId: Long, photoId: Long): PhotoImage?
    fun deleteOwnedPhotos(userId: Long, photoIds: List<Long>): List<PhotoImage>

    fun getOwnedPhoto(userId: Long, photoId: Long): PhotoImage?
}
