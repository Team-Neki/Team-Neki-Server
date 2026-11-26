package com.neki.domain.share.external

import com.neki.domain.share.models.SharePhoto
import com.neki.domain.share.models.SharedImageContent
import java.time.Duration

/**
 * fileName       : SharedImageDelivery
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 이미지 전달 방식 추상화 (서버 프록시 / CloudFront signed URL 리다이렉트)
 * - 구현체를 바꿔도 공유 URL(/share/{token}) 형식은 바뀌지 않는다.
 */
interface SharedImageDelivery {

    /** validFor: 이 응답이 유효해도 되는 최대 시간 (캐시 max-age, CloudFront 서명 만료에 사용) */
    fun deliver(photo: SharePhoto, validFor: Duration): SharedImageContent
}
