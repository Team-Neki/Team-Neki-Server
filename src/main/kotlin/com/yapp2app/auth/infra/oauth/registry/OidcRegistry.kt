package com.yapp2app.auth.infra.oauth.registry

import com.yapp2app.auth.infra.oauth.oidc.AppleOidc
import com.yapp2app.auth.infra.oauth.oidc.KakaoOidc
import com.yapp2app.auth.infra.oauth.oidc.Oidc
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.domain.enums.ProviderType
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
