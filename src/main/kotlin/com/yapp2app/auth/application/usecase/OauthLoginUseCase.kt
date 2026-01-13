package com.yapp2app.auth.application.usecase

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.yapp2app.auth.application.command.RegisterOauthUserCommand
import com.yapp2app.auth.application.contract.AuthCacheKeys
import com.yapp2app.auth.application.contract.GetKakaoTokenResponse
import com.yapp2app.auth.application.contract.OauthInfoResponse
import com.yapp2app.auth.application.port.AuthCachePort
import com.yapp2app.auth.application.port.AuthTokenProviderPort
import com.yapp2app.auth.application.port.OauthHelperPort
import com.yapp2app.auth.application.port.OidcPort
import com.yapp2app.auth.application.result.GetAuthResult
import com.yapp2app.auth.infra.oauth.OauthHelperAdapterRegistry
import com.yapp2app.auth.infra.oauth.OidcAdapterRegistry
import com.yapp2app.auth.infra.security.properties.OauthProperties
import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.user.application.port.UserRepositoryPort
import com.yapp2app.user.domain.entity.User
import com.yapp2app.user.domain.enums.RoleType
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

/**
 * fileName       : KaKaoAuthUseCase
 * author         : darren
 * date           : 2025. 12. 26. 18:08
 * description    : 카카오 auth usecase
 */
@UseCase
class OauthLoginUseCase(
    private val oauthProperties: OauthProperties,
    private val oidcAdapterRegistry: OidcAdapterRegistry,
    private val oauthHelperAdapterRegistry: OauthHelperAdapterRegistry,
    private val cachePort: AuthCachePort,
    private val restClient: RestClient,
    private val tokenProviderPort: AuthTokenProviderPort,
    private val userRepositoryPort: UserRepositoryPort,
    private val transactionRunner: TransactionRunner,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 1. ProviderType에 따라 적절한 Adapter 선택
     * 2. 공개키 조회
     * 3. ID Token 검증 및 Claims 추출
     * 4. oauthInfoResult 값 여부에 따라 회원가입 처리
     */
    fun execute(command: RegisterOauthUserCommand): GetAuthResult {
        // Registry로부터 providerType에 맞는 Adapter 선택
        val oidcAdapter = oidcAdapterRegistry.getAdapter(command.providerType)
        val oauthHelperAdapter = oauthHelperAdapterRegistry.getAdapter(command.providerType)

        val oauthInfoResponse = validateTokenWithCacheRetry(command.idToken, oidcAdapter, oauthHelperAdapter)

        // 회원가입 또는 로그인
        val user = transactionRunner.run { registerOauthUserIfEmpty(oauthInfoResponse) }

        // JWT 토큰 생성
        val accessToken = tokenProviderPort.createAccessToken(
            id = user.id.toString(),
            roles = user.roles.split(","),
            name = user.name,
            providerType = user.providerType,
        )

        val refreshToken = tokenProviderPort.createRefreshToken(
            id = user.id.toString(),
            roles = user.roles.split(","),
            name = user.name,
            providerType = user.providerType,
        )

        return GetAuthResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    /**
     * 캐시된 공개키로 토큰 검증 시도 및 실패 시 재시도 로직
     * - 1차 시도: 캐시된 공개키로 토큰 검증
     * - BusinessException 발생 시: 캐시 무효화 후 재시도 (공개키 로테이션 대응)
     */
    private fun validateTokenWithCacheRetry(
        idToken: String,
        oidcPort: OidcPort,
        oauthHelperPort: OauthHelperPort,
    ): OauthInfoResponse = try {
        // 1차 시도: 캐시된 공개키로 토큰 검증
        validateTokenWithPublicKeys(idToken, oidcPort, oauthHelperPort)
    } catch (e: BusinessException) {
        log.info("Kakao OIDC token expired. 갱신 로직")
        // 캐시 무효화 후 재시도
        cachePort.clearPublicKeys(AuthCacheKeys.KAKAO_OIDC_KEY)
        validateTokenWithPublicKeys(idToken, oidcPort, oauthHelperPort)
    } catch (e: Exception) {
        e.printStackTrace() // TODO 인증 실패 예외 로그 모니터링용
        throw e
    }

    /**
     * OIDC 공개키를 조회하여 ID Token을 검증합니다.
     * - 공개키는 Redis에 캐싱되어 있을 수 있음
     * - 검증 실패 시 BusinessException 발생
     */
    private fun validateTokenWithPublicKeys(
        idToken: String,
        oidcPort: OidcPort,
        oauthHelperPort: OauthHelperPort,
    ): OauthInfoResponse {
        val publicKeys = oidcPort.getOIDCPublicKey()
        return oauthHelperPort.getOauthInfoByIdToken(
            idToken = idToken,
            publicKeys = publicKeys,
        )
    }

    private fun registerOauthUserIfEmpty(oauthInfoResponse: OauthInfoResponse): User {
        val user = userRepositoryPort.findByOid(
            oid = oauthInfoResponse.oid,
            provider = oauthInfoResponse.providerType,
        ) ?: User(
            email = oauthInfoResponse.email,
            oid = oauthInfoResponse.oid,
            name = oauthInfoResponse.name,
            roles = RoleType.USER.role,
            providerType = oauthInfoResponse.providerType,
            imageUrl = oauthInfoResponse.imageUrl,
        )

        return userRepositoryPort.save(user)
    }

    /**
     * [TEST 용도]
     * idToken값을 APP 없이 추출하기 위한 코드
     * 카카오 인가 코드를 사용하여 액세스 토큰을 획득합니다.
     *
     * @param code 카카오 인증 서버에서 발급받은 인가 코드
     * @return KakaoTokenResponse 카카오 토큰 정보
     * @throws Exception 토큰 획득 실패 시
     */
    fun getAccessTokenByCode(code: String): GetKakaoTokenResponse {
        val clientId = oauthProperties.kakao.clientId
        val clientSecret = oauthProperties.kakao.clientSecret

        val params = LinkedMultiValueMap<String, String>()
        params.add("grant_type", "authorization_code")
        params.add("client_id", clientId)
        params.add("code", code)

        // Client Secret이 있으면 추가
        if (clientSecret.isNotBlank()) {
            params.add("client_secret", clientSecret)
        }

        val response = restClient.post()
            .uri("https://kauth.kakao.com/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(params)
            .retrieve()
            .body(String::class.java) ?: throw RuntimeException("Failed to get token from Kakao")

        // JSON 응답을 파싱하여 KakaoTokenResponse로 변환
        val objectMapper = jacksonObjectMapper()
        val jsonNode = objectMapper.readTree(response)

        return GetKakaoTokenResponse(
            accessToken = jsonNode.get("access_token").asText(),
            tokenType = jsonNode.get("token_type").asText(),
            refreshToken = jsonNode.get("refresh_token").asText(),
            expiresIn = jsonNode.get("expires_in").asInt(),
            scope = jsonNode.get("scope")?.asText(),
            refreshTokenExpiresIn = jsonNode.get("refresh_token_expires_in")?.asInt(),
            idToken = jsonNode.get("id_token")?.asText(),
        )
    }
}
