package com.neki.domain.share.client

import com.neki.domain.share.models.SharedImageContent

/**
 * fileName       : MediaClient
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : share 도메인이 media 도메인에서 이미지 바이너리를 받는 인터페이스
 */
interface MediaClient {

    fun fetchImage(storageKey: String): SharedImageContent.Binary
}
