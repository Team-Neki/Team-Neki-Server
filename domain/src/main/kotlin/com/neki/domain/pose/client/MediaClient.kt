package com.neki.domain.pose.client

import com.neki.domain.pose.models.MediaAvailability
import com.neki.domain.pose.models.MediaMetadata
import com.neki.domain.pose.models.PoseImageFile
import com.neki.domain.pose.models.PoseImageStoreOutcome

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

    /**
     * 일괄 업로드 이미지 한 장을 스토리지에 올리고 UPLOADED 상태의 포즈 media 로 저장한다.
     * 이미지가 아니거나 저장에 실패해도 예외 대신 [PoseImageStoreOutcome.Failed] 로 알린다.
     */
    fun storePoseImage(ownerId: Long, image: PoseImageFile): PoseImageStoreOutcome

    /**
     * 보상: [storePoseImage] 로 만든 media 를 스토리지에서 지우고 DELETED 로 표시한다. 실패하면 예외를 던진다.
     */
    fun discardPoseImage(ownerId: Long, mediaId: Long)
}
