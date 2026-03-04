package com.neki.user.infra.security.oauth

import com.neki.common.exception.BusinessException
import com.neki.user.contract.AuthCacheKeys
import com.neki.user.contract.OIDCPublicKeysPayload
import com.neki.user.contract.OauthInfoPayload
import com.neki.user.application.port.OidcTokenValidatorPort
import com.neki.user.domain.enums.Platform
import com.neki.user.domain.enums.ProviderType
import com.neki.user.infra.cache.AuthRedisCacheAdapter
import com.neki.user.infra.security.oauth.helper.OauthHelper
import com.neki.user.infra.security.oauth.oidc.Oidc
import com.neki.user.infra.security.oauth.registry.OauthHelperRegistry
import com.neki.user.infra.security.oauth.registry.OidcRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * fileName       : OidcTokenValidator
 * author         : darren
 * date           : 2026. 1. 14. 17:41
 * description    :
 */
@Component
class OidcTokenValidator(
    private val oidcRegistry: OidcRegistry,
    private val oauthHelperRegistry: OauthHelperRegistry,
    private val authRedisCacheAdapter: AuthRedisCacheAdapter,
) : OidcTokenValidatorPort {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 캐시된 공개키로 토큰 검증 시도 및 실패 시 재시도 로직
     * - 1차 시도: 캐시된 공개키로 토큰 검증
     * - BusinessException 발생 시: 캐시 무효화 후 재시도 (공개키 로테이션 대응)
     */
    override fun validateIdToken(idToken: String, providerType: ProviderType, platform: Platform): OauthInfoPayload {
        val oidcAdapter: Oidc = oidcRegistry.getAdapter(providerType)
        val oauthHelperAdapter: OauthHelper = oauthHelperRegistry.getAdapter(providerType)

        return try {
            // 1차 시도: 캐시된 공개키로 토큰 검증
            validateTokenWithPublicKeys(idToken, oidcAdapter, oauthHelperAdapter, platform)
        } catch (e: BusinessException) {
            // 캐시 무효화 후 재시도
            authRedisCacheAdapter.clearPublicKeys(AuthCacheKeys.KAKAO_OIDC_KEY)
            validateTokenWithPublicKeys(idToken, oidcAdapter, oauthHelperAdapter, platform)
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * OIDC 공개키를 조회하여 ID Token을 검증합니다.
     * - 공개키는 Redis에 캐싱되어 있을 수 있음
     * - 검증 실패 시 BusinessException 발생
     */
    private fun validateTokenWithPublicKeys(
        idToken: String,
        oidc: Oidc,
        oauthHelper: OauthHelper,
        platform: Platform,
    ): OauthInfoPayload {
        val publicKeys: OIDCPublicKeysPayload = oidc.getOIDCPublicKey()
        return oauthHelper.getOauthInfoByIdToken(
            idToken = idToken,
            publicKeys = publicKeys,
            platform = platform,
        )
    }
}
