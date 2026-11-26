package com.neki.domain.share.infra.delivery

import com.neki.domain.share.client.MediaClient
import com.neki.domain.share.external.SharedImageDelivery
import com.neki.domain.share.models.SharePhoto
import com.neki.domain.share.models.SharedImageContent
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * fileName       : ProxySharedImageDeliveryAdapter
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 이미지를 서버가 직접 읽어 내려주는 전달 방식.
 *
 * 전달 방식(서버 프록시 / CloudFront signed URL 리다이렉트)의 교체점이라 external/SharedImageDelivery 뒤에 둔다.
 * 프록시 구현은 media 바이너리가 필요하므로 같은 도메인의 share.client.MediaClient 로 받는다
 * (교차 도메인 호출은 client/ 로만 한다는 규칙 유지). CloudFront 구현은 storageKey 만으로 서명 URL 을 만든다.
 */
@Component
class ProxySharedImageDeliveryAdapter(private val mediaClient: MediaClient) : SharedImageDelivery {

    override fun deliver(photo: SharePhoto, validFor: Duration): SharedImageContent =
        mediaClient.fetchImage(photo.storageKey)
}
