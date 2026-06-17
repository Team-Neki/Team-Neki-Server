package com.neki.user.infra.security.oauth.registry

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.enums.ProviderType
import com.neki.user.infra.security.oauth.oidc.Oidc
import org.springframework.stereotype.Component

/**
 * fileName       : OidcRegistry
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : ProviderType별 Oidc 구현체를 관리하는 Registry
 */
@Component
class OidcRegistry(oidcAdapters: List<Oidc>) {
    private val adapters: Map<ProviderType, Oidc> = oidcAdapters.associateBy { it.providerType }

    fun getAdapter(providerType: ProviderType): Oidc = adapters[providerType]
        ?: throw BusinessException(ResultCode.INVALID_PARAMETER)
}
