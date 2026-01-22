package com.yapp2app.auth.infra.oauth.registry

import com.yapp2app.auth.infra.oauth.helper.AppleOauthHelper
import com.yapp2app.auth.infra.oauth.helper.KakaoOauthHelper
import com.yapp2app.auth.infra.oauth.helper.OauthHelper
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.domain.enums.ProviderType
import org.springframework.stereotype.Component

/**
 * fileName       : OauthHelperRegistry
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : ProviderType별 OauthHelper 구현체를 관리하는 Registry
 */
@Component
class OauthHelperRegistry(
    private val kakaoOauthHelper: KakaoOauthHelper,
    private val appleOauthHelper: AppleOauthHelper,
) {
    private val adapters: Map<ProviderType, OauthHelper> = mapOf(
        ProviderType.KAKAO to kakaoOauthHelper,
        ProviderType.APPLE to appleOauthHelper,
    )

    fun getAdapter(providerType: ProviderType): OauthHelper = adapters[providerType]
        ?: throw BusinessException(ResultCode.INVALID_PARAMETER)
}
