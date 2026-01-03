package com.yapp2app.auth.application.usecase

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.yapp2app.auth.application.command.RegisterKakaoUserCommand
import com.yapp2app.auth.application.port.OauthHelperPort
import com.yapp2app.auth.application.port.OidcPort
import com.yapp2app.auth.application.result.GetKakaoRegisterResult
import com.yapp2app.auth.infra.response.GetKakaoTokenResponse
import com.yapp2app.auth.infra.response.OauthInfoResponse
import com.yapp2app.auth.infra.security.properties.OauthProperties
import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.infra.TransactionRunner
import com.yapp2app.user.application.port.UserRepositoryPort
import com.yapp2app.user.domain.entity.User
import com.yapp2app.user.domain.enums.RoleType
import org.springframework.beans.factory.annotation.Qualifier
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
class KakaoRegisterUseCase(
    private val oauthProperties: OauthProperties,
    @Qualifier("kakaoOidcAdapter") private val oidcPort: OidcPort,
    @Qualifier("kakaoOauthHelper") private val oauthHelperPort: OauthHelperPort,
    private val restClient: RestClient,
    private val userRepositoryPort: UserRepositoryPort,
    private val transactionRunner: TransactionRunner,
) {

    /**
     * 1. 카카오 공개키 조회
     * 2. ID Token 검증 및 Claims 추출
     * 3. oauthInfoResult 값 여부에 따라 회원가입 처리
     */
    fun execute(command: RegisterKakaoUserCommand): GetKakaoRegisterResult {
        // 카카오 공개 키 가져오기
        val oidcPublicKeysResult = oidcPort.getOIDCPublicKey()

        // ID Token 검증 및 Claims 추출
        val oauthInfoResponse: OauthInfoResponse = oauthHelperPort.getOauthInfoByIdToken(
            idToken = command.idToken,
            publicKeys = oidcPublicKeysResult,
        )

        val user = transactionRunner.run { registerKakaoUserIfEmpty(oauthInfoResponse) }

        return GetKakaoRegisterResult(oid = user.oid, providerType = user.providerType)
    }

    private fun registerKakaoUserIfEmpty(oauthInfoResponse: OauthInfoResponse): User {
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
        val clientId = "a8777a62d28eee709e96cd6f803ec377"
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
