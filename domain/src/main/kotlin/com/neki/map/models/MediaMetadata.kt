package com.neki.map.models

/**
 * fileName       : MediaMetadata
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 브랜드 로고 등 media의 메타데이터. 어댑터가 media 도메인 응답을 변환해 넘겨준다.
 */
data class MediaMetadata(
    val mediaId: Long,
    val storageKey: String,
    val contentType: String,
    val width: Int? = null,
    val height: Int? = null,
)
