package com.neki.user.infra.security.oauth.registry

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.enums.ProviderType
import com.neki.user.infra.security.oauth.helper.OauthHelper
import org.springframework.stereotype.Component

/**
 * fileName       : OauthHelperRegistry
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : ProviderType별 OauthHelper 구현체를 관리하는 Registry
 */
@Component
class OauthHelperRegistry(oauthHelpers: List<OauthHelper>) {
    private val adapters: Map<ProviderType, OauthHelper> = oauthHelpers.associateBy { it.providerType }

    fun getAdapter(providerType: ProviderType): OauthHelper = adapters[providerType]
        ?: throw BusinessException(ResultCode.INVALID_PARAMETER)
}
