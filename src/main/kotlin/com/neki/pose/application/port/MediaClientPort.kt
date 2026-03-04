package com.neki.pose.application.port

import com.neki.pose.contract.MediaAvailability
import com.neki.pose.contract.MediaStorageInfo

/**
 * fileName       : MediaClientPort
 * author         : darren
 * date           : 2026. 1. 27. 17:14
 * description    :
 */
interface MediaClientPort {

    fun getMediaStorageInfo(mediaId: Long): MediaStorageInfo

    fun getMediaStorageInfos(mediaIds: List<Long>): List<MediaStorageInfo>

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
