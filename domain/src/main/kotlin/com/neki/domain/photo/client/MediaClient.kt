package com.neki.domain.photo.client

import com.neki.domain.photo.models.MediaAvailability
import com.neki.domain.photo.models.MediaMetadata

/**
 * fileName       : MediaClient
 * author         : koo
 * date           : 2026. 1. 2. 오후 11:58
 * description    : Media Client 호출을 위한 인터페이스
 */
interface MediaClient {

    fun getMediaMetadata(ownerId: Long, mediaId: Long): MediaMetadata

    fun getMediaMetadata(ownerId: Long, mediaIds: List<Long>): List<MediaMetadata>

    fun deleteMedias(ownerId: Long, mediaIds: List<Long>)

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
