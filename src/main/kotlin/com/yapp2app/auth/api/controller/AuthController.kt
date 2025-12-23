package com.yapp2app.auth.api.controller

import com.yapp2app.auth.api.dto.KakaoOIDCLoginRequest
import com.yapp2app.auth.api.dto.TokenResponse
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.common.properties.OauthProperties
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient

/**
 * fileName       : AuthController
 * author         : darren
 * date           : 2025. 12. 12. 13:18
 * description    : 인증/인가 관련 API
 */
@Tag(name = "AuthController", description = "인증/인가 API")
@RequestMapping("/api/auth")
@RestController
class AuthController(var oauthProperties: OauthProperties) {

    /**
     * OIDC 방식 로그인
     */
    @Operation(
        summary = "카카오 OIDC 로그인",
        description = """
        ## 카카오 OIDC 로그인 API

        앱에서 카카오 SDK로 획득한 idToken을 검증하고 회원가입/로그인을 처리합니다.

        ### 테스트용 idToken 발급 방법

        #### 1단계: Authorization Code 획득
        아래 URL을 브라우저에서 실행하여 카카오 로그인 후 idToken 얻습니다.

        https://kauth.kakao.com/oauth/authorize?client_id=a8777a62d28eee709e96cd6f803ec377&redirect_uri=http://localhost:8080/api/auth/test/kakao/redirect&response_type=code&scope=openid,profile_nickname

        응답의 `id_token` 필드 값을 이 API의 `idToken`으로 사용하세요.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "카카오 OIDC 엔드포인트가 정상적으로 작동합니다."),
    )
    @PostMapping("/kakao/oidc")
    fun kakaoLoginWithOIDC(@RequestBody request: KakaoOIDCLoginRequest): BaseResponse<TokenResponse> =
        BaseResponse(data = TokenResponse("OK", "OK"))

    /**
     * ****** Test용이므로 Swagger Hidden 처리 ******
     * 테스트용 카카오 OAuth Redirect 엔드포인트
     * Authorization Code를 받아서 idToken으로 교환
     */
    @Hidden
    @GetMapping("/test/kakao/redirect")
    fun kakaoTestRedirect(@RequestParam code: String): ResponseEntity<String> {
        val clientId = "a8777a62d28eee709e96cd6f803ec377"
        val clientSecret = oauthProperties.kakao.clientSecret // 카카오 디벨로퍼스에서 Client Secret을 확인하여 입력 (선택사항)
        val redirectUri = "http://localhost:8080/api/auth/test/kakao/redirect"

        val restClient = RestClient.create()

        val params = LinkedMultiValueMap<String, String>()
        params.add("grant_type", "authorization_code")
        params.add("client_id", clientId)
        params.add("redirect_uri", redirectUri)
        params.add("code", code)

        // Client Secret이 있으면 추가
        if (clientSecret.isNotBlank()) {
            params.add("client_secret", clientSecret)
        }

        return try {
            val response = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(String::class.java)

            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"error": "${e.message}"}""")
        }
    }
}
