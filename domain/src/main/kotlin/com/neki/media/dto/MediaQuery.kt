package com.neki.media.dto

/**
 * fileName       : MediaQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Media domain query
 */
object MediaQuery {
    data class GetMedias(val ownerId: Long, val mediaIds: List<Long>)

    data class GetImageByKey(val objectKey: String)

    data class GetMediaMetadata(val ownerId: Long?, val mediaId: Long)

    data class GetMediaMetadataList(val ownerId: Long?, val mediaIds: List<Long>)
}
