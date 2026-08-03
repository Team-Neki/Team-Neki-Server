package com.neki.pose

import com.neki.pose.models.MediaAvailability
import com.neki.pose.models.MediaMetadata

/**
 * fileName       : MediaClient
 * author         : darren
 * date           : 2026. 1. 27. 17:14
 * description    :
 */
interface MediaClient {

    fun getMediaMetadata(mediaId: Long): MediaMetadata

    fun getMediaMetadata(mediaIds: List<Long>): List<MediaMetadata>

    /**
     * 여러 media가 object storage에 정상적으로 저장되었는지 확인
     * @return mediaId와 가용 여부의 Map
     */
    fun verifyMediasUploaded(ownerId: Long, mediaIds: List<Long>): Map<Long, MediaAvailability>

    /**
     * 보상 트랜잭션: 여러 media 상태를 INITIATED로 롤백
     * PhotoImage 저장 실패 시 호출
     */
    fun rollbackMediasUploaded(ownerId: Long, mediaIds: List<Long>)
}
