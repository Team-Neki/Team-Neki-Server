package com.neki.domain.share.infra.token

import com.neki.domain.share.ShareTokenPolicy
import com.neki.domain.share.external.ShareTokenGenerator
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

/**
 * fileName       : SecureRandomShareTokenGeneratorAdapter
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : SecureRandom 32바이트(256bit)를 Base64 URL-safe 무패딩으로 인코딩한 공유 토큰 생성
 */
@Component
class SecureRandomShareTokenGeneratorAdapter : ShareTokenGenerator {

    private val secureRandom = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    override fun generate(): String {
        val bytes = ByteArray(ShareTokenPolicy.TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }
}
