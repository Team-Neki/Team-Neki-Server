package com.neki.domain.media.models

/**
 * fileName       : ImageMetadata
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : 이미지 바이트에서 직접 읽어낸 형식과 크기
 */
data class ImageMetadata(val contentType: String, val extension: String, val width: Int, val height: Int)
