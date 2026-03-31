package com.neki.user.infra.security.oauth.registry

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.domain.enums.ProviderType
import com.neki.user.infra.security.oauth.helper.AppleOauthHelper
import com.neki.user.infra.security.oauth.helper.KakaoOauthHelper
import com.neki.user.infra.security.oauth.helper.OauthHelper
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
