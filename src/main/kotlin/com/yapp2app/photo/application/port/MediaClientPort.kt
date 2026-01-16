package com.yapp2app.photo.application.port

import com.yapp2app.photo.application.contract.MediaAvailability
import com.yapp2app.photo.application.contract.MediaInfo

/**
 * fileName       : MediaClient
 * author         : koo
 * date           : 2026. 1. 2. 오후 11:58
 * description    : Media Client 호출을 위한 인터페이스
 */
interface MediaClientPort {

    fun verifyMediaUploaded(ownerId: Long, mediaId: Long): MediaAvailability

    fun getMediaBinaries(ownerId: Long, mediaIds: List<Long>): List<MediaInfo>

    fun deleteMedia(ownerId: Long, mediaId: Long)

    fun deleteMedias(ownerId: Long, mediaIds: List<Long>)
}
