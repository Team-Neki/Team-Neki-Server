package com.neki.user.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.user.application.dto.AuthResult
import com.neki.user.dto.AuthCommand
import com.neki.user.external.UserEventPublisher
import com.neki.user.infra.security.config.OauthProperties
import com.neki.user.models.IssuedTokens
import com.neki.user.models.OauthRegistration
import com.neki.user.models.OauthUserInfo
import com.neki.user.models.UserRegisteredEvent
import com.neki.user.service.AuthService
import com.neki.user.service.UserService
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
    private val restClient: RestClient,
    private val authService: AuthService,
    private val userService: UserService,
    private val userEventPublisher: UserEventPublisher,

    private val transactionRunner: TransactionRunner,
) {

    /**
     * 1. ID Token 검증 및 OAuth 사용자 정보 추출
     * 2. 미가입 사용자면 회원가입 처리
     * 3. 신규 가입 시 이벤트 발행
     * 4. 토큰 발급
     */
    fun execute(command: AuthCommand.RegisterOauthUser): AuthResult.GetOauthLogin {
        val oauthUserInfo: OauthUserInfo = authService.validateOauthToken(command)

        // 신규 사용자 추가
        val registration: OauthRegistration = transactionRunner.run {
            userService.registerOauthUserIfAbsent(oauthUserInfo)
        }

        // 신규 사용자 알림 (트랜잭션 커밋 이후 비동기 처리)
        if (registration.isNew) {
            val activeUserCount: Long = userService.countActiveUsers()

            userEventPublisher.publish(
                UserRegisteredEvent(
                    userId = registration.user.id!!,
                    nickname = registration.user.name!!,
                    providerType = registration.user.providerType.name,
                    platform = command.platform.value,
                    activeUserCount = activeUserCount,
                ),
            )
        }

        // 토큰 생성
        val tokens: IssuedTokens = authService.issueTokens(registration.user)

        return AuthResult.GetOauthLogin(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            isNewUser = registration.isNew,
        )
    }

    /**
     * [TEST 용도]
     * idToken값을 APP 없이 추출하기 위한 코드
     * 카카오 인가 코드를 사용하여 액세스 토큰을 획득합니다.
     *
     * @param code 카카오 인증 서버에서 발급받은 인가 코드
     * @return AuthResult.KakaoToken 카카오 토큰 정보
     * @throws Exception 토큰 획득 실패 시
     */
    fun getAccessTokenByCode(code: String): AuthResult.KakaoToken {
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

        // JSON 응답을 파싱하여 KakaoToken으로 변환
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        val jsonNode: JsonNode = objectMapper.readTree(response)

        return AuthResult.KakaoToken(
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
