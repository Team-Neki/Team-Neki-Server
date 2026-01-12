package com.yapp2app.auth.infra.oauth

import com.yapp2app.auth.application.port.OauthHelperPort
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.domain.enums.ProviderType
import org.springframework.stereotype.Component

/**
 * fileName       : OauthHelperAdapterRegistry
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : ProviderType별 OauthHelperPort 구현체를 관리하는 Registry
 */
@Component
class OauthHelperAdapterRegistry(
    private val kakaoOauthHelper: KakaoOauthHelper,
    private val appleOauthHelper: AppleOauthHelper,
) {
    private val adapters: Map<ProviderType, OauthHelperPort> = mapOf(
        ProviderType.KAKAO to kakaoOauthHelper,
        ProviderType.APPLE to appleOauthHelper,
    )

    fun getAdapter(providerType: ProviderType): OauthHelperPort = adapters[providerType]
        ?: throw BusinessException(ResultCode.INVALID_PARAMETER)
}
