package com.neki.domain.map.client

import com.neki.domain.map.models.MediaMetadata

/**
 * fileName       : MediaClient
 * author         : darren
 * date           : 2026. 1. 22
 * description    : Media Client 호출을 위한 인터페이스
 */
interface MediaClient {

    fun getMediaMetadata(mediaIds: List<Long>): List<MediaMetadata>
}
