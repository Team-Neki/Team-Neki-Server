package com.neki.user.client

import com.neki.user.models.MediaAvailability

/**
 * fileName       : MediaClient
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:59
 * description    :
 */
interface MediaClient {

    fun deleteMedia(ownerId: Long, mediaIds: Long)

    fun verifyMediaUploaded(ownerId: Long, mediaId: Long): MediaAvailability

    /**
     * 보상 트랜잭션: 여러 media 상태를 INITIATED로 롤백
     * PhotoImage 저장 실패 시 호출
     */
    fun rollbackMediasUploaded(ownerId: Long, mediaIds: List<Long>)

    /**
     * mediaId로 storage key 조회
     * @return storageKey 또는 null (미존재 시)
     */
    fun getStorageKey(ownerId: Long, mediaId: Long): String?
}
