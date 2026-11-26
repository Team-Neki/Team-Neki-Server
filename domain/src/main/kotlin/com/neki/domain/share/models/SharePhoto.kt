package com.neki.domain.share.models

/**
 * fileName       : SharePhoto
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 대상 사진. storageKey 는 이미지 전달에만 쓰고 응답에 절대 넣지 않는다.
 */
data class SharePhoto(val photoId: Long, val userId: Long, val storageKey: String)
