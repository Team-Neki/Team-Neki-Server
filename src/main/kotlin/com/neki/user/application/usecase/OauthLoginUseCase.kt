package com.neki.user.application.usecase

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.user.application.command.RegisterOauthUserCommand
import com.neki.user.application.contract.KakaoTokenPayload
import com.neki.user.application.contract.OauthInfoPayload
import com.neki.user.application.port.AuthTokenProviderPort
import com.neki.user.application.port.NicknameGeneratorPort
import com.neki.user.application.port.OidcTokenValidatorPort
import com.neki.user.application.port.UserEventPublisherPort
import com.neki.user.application.port.UserRepositoryPort
import com.neki.user.application.result.GetAuthResult
import com.neki.user.domain.entity.User
import com.neki.user.domain.enums.RoleType
import com.neki.user.event.UserRegisteredEvent
import com.neki.user.infra.security.config.OauthProperties
import org.slf4j.Logger
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
    private val oidcTokenValidatorPort: OidcTokenValidatorPort,
    private val restClient: RestClient,
    private val tokenProviderPort: AuthTokenProviderPort,
    private val userRepositoryPort: UserRepositoryPort,
    private val nicknameGenerator: NicknameGeneratorPort,
    private val userEventPublisher: UserEventPublisherPort,

    private val transactionRunner: TransactionRunner,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 1. ProviderType에 따라 적절한 Adapter 선택
     * 2. 공개키 조회
     * 3. ID Token 검증 및 Claims 추출
     * 4. oauthInfoResult 값 여부에 따라 회원가입 처리
     */
    fun execute(command: RegisterOauthUserCommand): GetAuthResult {
        val oauthInfoPayload: OauthInfoPayload = oidcTokenValidatorPort.validateIdToken(
            command.idToken,
            command.providerType,
            command.platform,
        )

        // 신규 사용자 추가
        val (user, isNew) = transactionRunner.run { registerOauthUserIfEmpty(oauthInfoPayload) }

        // 신규 사용자 알림 (트랜잭션 커밋 이후 비동기 처리)
        if (isNew) {
            val activeUserCount: Long = userRepositoryPort.countByOidIsNotNull()

            userEventPublisher.publish(
                UserRegisteredEvent(
                    userId = user.id!!,
                    nickname = user.name!!,
                    providerType = user.providerType.name,
                    platform = command.platform.value,
                    activeUserCount = activeUserCount,
                ),
            )
        }

        // 토큰 생성
        val accessToken: String = tokenProviderPort.createAccessToken(
            id = user.id.toString(),
            roles = user.roles.split(","),
            name = user.name,
            providerType = user.providerType,
        )

        val refreshToken: String = tokenProviderPort.createRefreshToken(
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

    private fun registerOauthUserIfEmpty(oauthInfoPayload: OauthInfoPayload): Pair<User, Boolean> {
        val existingUser: User? = userRepositoryPort.findByOid(
            oid = oauthInfoPayload.oid,
            provider = oauthInfoPayload.providerType,
        )

        return if (existingUser != null) {
            Pair(existingUser, false)
        } else {
            val nickname: String = nicknameGenerator.generateUniqueNickname()
            val newUser: User = userRepositoryPort.save(
                User(
                    email = oauthInfoPayload.email,
                    oid = oauthInfoPayload.oid,
                    name = nickname,
                    roles = RoleType.USER.role,
                    providerType = oauthInfoPayload.providerType,
                    profileImageId = null,
                ),
            )
            Pair(newUser, true)
        }
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
    fun getAccessTokenByCode(code: String): KakaoTokenPayload {
        val clientId = oauthProperties.kakao.androidClientId
        val clientSecret = oauthProperties.kakao.clientSecret

        val params = LinkedMultiValueMap<String, String>()
        params.add("grant_type", "authorization_code")
        params.add("client_id", clientId)
        params.add("code", code)

        // Client Secret이 있으면 추가
        if (clientSecret.isNotBlank()) {
            params.add("client_secret", clientSecret)
        }

        val response: String = restClient.post()
            .uri("https://kauth.kakao.com/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(params)
            .retrieve()
            .body(String::class.java) ?: throw RuntimeException("Failed to get token from Kakao")

        // JSON 응답을 파싱하여 KakaoTokenResponse로 변환
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        val jsonNode: JsonNode = objectMapper.readTree(response)

        return KakaoTokenPayload(
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
