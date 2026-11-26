package com.neki.domain.share.client

import com.neki.domain.share.models.SharePhoto

/**
 * fileName       : PhotoClient
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : share 도메인이 photo 도메인을 호출하는 인터페이스
 */
interface PhotoClient {

    /** 소유자 userId 의 살아있는 사진 + UPLOADED media. 없거나 타인 소유거나 삭제됐으면 null */
    fun findSharablePhoto(userId: Long, photoId: Long): SharePhoto?
}
