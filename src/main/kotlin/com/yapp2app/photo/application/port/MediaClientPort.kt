package com.yapp2app.photo.application.port

import com.yapp2app.photo.application.contract.MediaAvailability
import com.yapp2app.photo.application.contract.MediaInfo
import com.yapp2app.photo.application.contract.MediaStorageInfo

/**
 * fileName       : MediaClient
 * author         : koo
 * date           : 2026. 1. 2. 오후 11:58
 * description    : Media Client 호출을 위한 인터페이스
 */
interface MediaClientPort {

    fun verifyMediaUploaded(ownerId: Long, mediaId: Long): MediaAvailability

    fun getMediaBinaries(ownerId: Long, mediaIds: List<Long>): List<MediaInfo>

    fun getMediaStorageInfos(ownerId: Long, mediaIds: List<Long>): List<MediaStorageInfo>

    fun deleteMedia(ownerId: Long, mediaId: Long)

    fun deleteMedias(ownerId: Long, mediaIds: List<Long>)

    /**
     * 보상 트랜잭션: media 상태를 INITIATED로 롤백
     * PhotoImage 저장 실패 시 호출
     */
    fun rollbackMediaUploaded(ownerId: Long, mediaId: Long)
}
