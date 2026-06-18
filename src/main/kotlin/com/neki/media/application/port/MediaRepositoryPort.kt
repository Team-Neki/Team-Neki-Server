package com.neki.media.application.port

import com.neki.media.domain.entity.Media

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

    /**
     * width/height/size 중 하나라도 null 인 UPLOADED 미디어를 id cursor 기반으로 조회한다. (백필용)
     */
    fun findMediaForDimensionBackfill(lastId: Long, limit: Int): List<Media>

    fun save(media: Media): Media
    fun saveAll(medias: List<Media>): List<Media>

    fun delete(id: Long)
    fun deleteAll(ids: List<Long>)
}
