package com.yapp2app.auth.infra.oauth

import com.yapp2app.auth.application.contract.AuthCacheKeys
import com.yapp2app.auth.application.contract.OauthInfoResponse
import com.yapp2app.auth.application.port.OidcTokenValidatorPort
import com.yapp2app.auth.infra.oauth.helper.OauthHelper
import com.yapp2app.auth.infra.oauth.oidc.Oidc
import com.yapp2app.auth.infra.oauth.registry.OauthHelperRegistry
import com.yapp2app.auth.infra.oauth.registry.OidcRegistry
import com.yapp2app.auth.infra.redis.AuthRedisCacheAdapter
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.domain.enums.ProviderType
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

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 캐시된 공개키로 토큰 검증 시도 및 실패 시 재시도 로직
     * - 1차 시도: 캐시된 공개키로 토큰 검증
     * - BusinessException 발생 시: 캐시 무효화 후 재시도 (공개키 로테이션 대응)
     */
    override fun validateIdToken(idToken: String, providerType: ProviderType, platform: String): OauthInfoResponse {
        val oidcAdapter = oidcRegistry.getAdapter(providerType)
        val oauthHelperAdapter = oauthHelperRegistry.getAdapter(providerType)

        return try {
            // 1차 시도: 캐시된 공개키로 토큰 검증
            validateTokenWithPublicKeys(idToken, oidcAdapter, oauthHelperAdapter, platform)
        } catch (e: BusinessException) {
            log.info("Kakao OIDC token expired. 갱신 로직")
            // 캐시 무효화 후 재시도
            authRedisCacheAdapter.clearPublicKeys(AuthCacheKeys.KAKAO_OIDC_KEY)
            validateTokenWithPublicKeys(idToken, oidcAdapter, oauthHelperAdapter, platform)
        } catch (e: Exception) {
            e.printStackTrace() // TODO 인증 실패 예외 로그 모니터링용
            throw e
        }
    }

    /**
     * OIDC 공개키를 조회하여 ID Token을 검증합니다.
     * - 공개키는 Redis에 캐싱되어 있을 수 있음
     * - 검증 실패 시 BusinessException 발생
     */
    private fun validateTokenWithPublicKeys(idToken: String, oidc: Oidc, oauthHelper: OauthHelper, platform: String): OauthInfoResponse {
        val publicKeys = oidc.getOIDCPublicKey()
        return oauthHelper.getOauthInfoByIdToken(
            idToken = idToken,
            publicKeys = publicKeys,
            platform = platform,
        )
    }
}
