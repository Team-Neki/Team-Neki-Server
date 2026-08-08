package com.neki.domain.media.repository

import com.neki.domain.media.models.Media

interface MediaRepository {

    fun getActiveMedia(id: Long): Media?
    fun getActiveMedia(ownerId: Long, id: Long): Media?
    fun getActiveMedias(ids: List<Long>): List<Media>
    fun getActiveMedias(ownerId: Long, ids: List<Long>): List<Media>
    fun getMediaForUploadConfirmation(ownerId: Long, ids: List<Long>): List<Media>

    fun save(media: Media): Media
    fun saveAll(medias: List<Media>): List<Media>

    fun delete(id: Long)
    fun deleteAll(ids: List<Long>)
}
