package com.yapp2app.auth.infra.oauth

import com.yapp2app.auth.application.port.OidcPort
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.domain.enums.ProviderType
import org.springframework.stereotype.Component

/**
 * fileName       : OidcAdapterRegistry
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : ProviderType별 OidcPort 구현체를 관리하는 Registry
 */
@Component
class OidcAdapterRegistry(
    private val kakaoOidcAdapter: KakaoOidcAdapter,
    private val appleOidcAdapter: AppleOidcAdapter,
) {
    private val adapters: Map<ProviderType, OidcPort> = mapOf(
        ProviderType.KAKAO to kakaoOidcAdapter,
        ProviderType.APPLE to appleOidcAdapter,
    )

    fun getAdapter(providerType: ProviderType): OidcPort = adapters[providerType]
        ?: throw BusinessException(ResultCode.INVALID_PARAMETER)
}
