package com.neki.domain.pose.models

/**
 * fileName       : PoseImageStoreOutcome
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : 일괄 업로드에서 이미지 한 장을 저장한 결과
 */
sealed interface PoseImageStoreOutcome {
    data class Stored(val mediaId: Long) : PoseImageStoreOutcome

    data class Failed(val reason: BulkFailReason) : PoseImageStoreOutcome
}
