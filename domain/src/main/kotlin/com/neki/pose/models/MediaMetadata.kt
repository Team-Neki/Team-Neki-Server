package com.neki.pose.models

/**
 * fileName       : MediaMetadata
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 포즈에 붙는 media의 메타데이터. 어댑터가 media 도메인 응답을 변환해 넘겨준다.
 */
data class MediaMetadata(
    val mediaId: Long,
    val storageKey: String,
    val contentType: String,
    val width: Int? = null,
    val height: Int? = null,
)

/**
 * mediaId로 조회하기 위한 메타데이터 묶음.
 * 아직 저장되지 않은 media는 조회 결과에 없으므로 null이 나온다.
 */
class MediaMetadatas(metadatas: List<MediaMetadata>) {
    private val byMediaId: Map<Long, MediaMetadata> = metadatas.associateBy { it.mediaId }

    operator fun get(mediaId: Long): MediaMetadata? = byMediaId[mediaId]
}
