package com.yapp2app.media.application.port

import com.yapp2app.media.domain.entity.Media

/**
 * fileName       : MediaRepositoryPort
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:06
 * description    : Media repository port
 */
interface MediaRepositoryPort {

    fun getActiveMedia(id: Long): Media?
    fun getActiveMedia(ownerId: Long, id: Long): Media?
    fun getActiveMedias(ids: List<Long>): List<Media>
    fun getActiveMedias(ownerId: Long, ids: List<Long>): List<Media>
    fun getMediaForUploadConfirmation(ownerId: Long, ids: List<Long>): List<Media>

    fun save(media: Media): Media

    fun delete(id: Long)
    fun deleteAll(ids: List<Long>)
}
