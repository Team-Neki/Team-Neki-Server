package com.neki.auth.infra.oauth.registry

import com.neki.auth.infra.oauth.oidc.AppleOidc
import com.neki.auth.infra.oauth.oidc.KakaoOidc
import com.neki.auth.infra.oauth.oidc.Oidc
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.domain.enums.ProviderType
import org.springframework.stereotype.Component

/**
 * fileName       : OidcRegistry
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : ProviderType별 Oidc 구현체를 관리하는 Registry
 */
@Component
class OidcRegistry(private val kakaoOidcAdapter: KakaoOidc, private val appleOidc: AppleOidc) {
    private val adapters: Map<ProviderType, Oidc> = mapOf(
        ProviderType.KAKAO to kakaoOidcAdapter,
        ProviderType.APPLE to appleOidc,
    )

    fun getAdapter(providerType: ProviderType): Oidc = adapters[providerType]
        ?: throw BusinessException(ResultCode.INVALID_PARAMETER)
}
