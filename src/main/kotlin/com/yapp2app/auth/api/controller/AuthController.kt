package com.yapp2app.auth.api.controller

import com.yapp2app.auth.api.request.KakaoRegisterRequest
import com.yapp2app.auth.api.request.LoginRequest
import com.yapp2app.auth.api.request.RefreshTokenRequest
import com.yapp2app.auth.api.response.GetKakaoRegisterResponse
import com.yapp2app.auth.api.response.GetKakaoTokenResponse
import com.yapp2app.auth.api.response.GetTokenResponse
import com.yapp2app.auth.application.command.LoginCommand
import com.yapp2app.auth.application.command.RefreshTokenCommand
import com.yapp2app.auth.application.command.RegisterKakaoUserCommand
import com.yapp2app.auth.application.usecase.KakaoRegisterUseCase
import com.yapp2app.auth.application.usecase.LoginUseCase
import com.yapp2app.auth.application.usecase.RefreshTokenUseCase
import com.yapp2app.common.api.dto.BaseResponse
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : AuthController
 * author         : darren
 * date           : 2025. 12. 12. 13:18
 * description    : 인증/인가 관련 API
 */
@Tag(name = "AuthController", description = "인증/인가 API")
@RequestMapping("/api/auth")
@RestController
class AuthController(
    private val kakaoRegisterUseCase: KakaoRegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
) {

    /**
     * OIDC 방식 회원가입
     */
    @Operation(
        summary = "카카오 OIDC 회원가입",
        description = """
        ## 카카오 OIDC 회원가입 API

        앱에서 카카오 SDK로 획득한 idToken을 검증하고 회원가입을 처리합니다.

        ### 테스트용 idToken 발급 방법

        #### 1단계: Authorization Code 획득
        아래 URL을 브라우저에서 실행하여 카카오 로그인 후 idToken 얻습니다.

        [local] https://kauth.kakao.com/oauth/authorize?client_id=a8777a62d28eee709e96cd6f803ec377&redirect_uri=http://localhost:8080/api/auth/test/kakao/redirect&response_type=code&scope=openid,profile_nickname

        [staging] https://kauth.kakao.com/oauth/authorize?client_id=a8777a62d28eee709e96cd6f803ec377&redirect_uri=https://dev-yapp.suitestudy.com:4641/api/auth/test/kakao/redirect&response_type=code&scope=openid,profile_nickname

        응답의 `id_token` 필드 값을 이 API의 `idToken`으로 사용하세요.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "카카오 OIDC 엔드포인트가 정상적으로 작동합니다."),
    )
    @PostMapping("/kakao/register")
    fun kakaoRegister(@RequestBody @Valid request: KakaoRegisterRequest): BaseResponse<GetKakaoRegisterResponse> {
        val result = kakaoRegisterUseCase.execute(RegisterKakaoUserCommand(idToken = request.idToken))

        return BaseResponse(data = GetKakaoRegisterResponse(oid = result.oid, providerType = result.providerType))
    }

    @Operation(
        summary = "로그인",
        description = """
        ## 로그인 API

        사용자의 OID와 ProviderType을 사용하여 로그인을 수행합니다.

        ### 성공 응답 (200 OK)
        - **accessToken**: API 요청에 사용할 액세스 토큰 (유효기간: 설정값에 따름)
        - **refreshToken**: 액세스 토큰 갱신에 사용할 리프레시 토큰 (유효기간: 설정값에 따름)

        ### API 호출 시 토큰 사용법
        ```
        Authorization: Bearer {accessToken}
        ```

        ### 토큰 만료 시 처리 방법
        1. 인가가 필요한 API 호출 시 **401 Unauthorized** 응답을 받은 경우
        2. 응답의 `code` 필드를 확인:
           - **D-997** (토큰 만료): `/api/auth/refresh` API로 토큰 갱신
           - **D-998** (토큰 무효): 재로그인 필요
           - **D-999** (인증 실패): 재로그인 필요

        ### 토큰 저장 권장사항
        - **accessToken**: 메모리 또는 안전한 저장소 (탈취 위험 최소화)
        - **refreshToken**: 안전한 저장소 (Keychain, EncryptedSharedPreferences 등)
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그인 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패 - 가입되지 않은 사용자"),
    )
    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): BaseResponse<GetTokenResponse> {
        val result = loginUseCase.execute(LoginCommand(oid = request.oid, providerType = request.providerType))

        return BaseResponse(data = GetTokenResponse(accessToken = result.accessToken, result.refreshToken))
    }

    @Operation(
        summary = "토큰 갱신",
        description = """
        ## AccessToken 갱신 API

        RefreshToken을 사용하여 새로운 AccessToken과 RefreshToken을 발급받습니다.

        ### 사용 시나리오
        1. 보호된 API 호출 시 **401 Unauthorized** 응답을 받음
        2. 응답의 `resultCode` 필드가 **D-997** (토큰 만료)인 경우
        3. 저장된 RefreshToken으로 이 API를 호출하여 새로운 토큰 발급

        ### Refresh Token Rotation (보안 강화)
        ⚠️ **중요**: 보안을 위해 Refresh Token Rotation을 적용합니다.
        - 새로운 **AccessToken**과 함께 새로운 **RefreshToken**도 함께 발급됩니다.
        - 기존 RefreshToken은 **즉시 무효화**되므로, 반드시 새로운 RefreshToken을 저장해야 합니다.
        - 같은 RefreshToken으로 두 번 요청하면 실패합니다.

        ### 성공 응답 (200 OK)
        ```json
        {
          "success": true,
          "data": {
            "accessToken": "새로운 AccessToken",
            "refreshToken": "새로운 RefreshToken (반드시 저장 필요!)"
          }
        }
        ```

        ### 실패 응답 (401 Unauthorized)

        #### D-998 (INVALID_TOKEN_ERROR) - RefreshToken 무효
        - **원인**: RefreshToken의 서명이 올바르지 않거나 형식이 잘못됨
        - **대응**: 재로그인 필요 → `/api/auth/login` 호출
        ```json
        {
          "code": "D-998",
          "message": "토큰이 올바르지 않습니다.",
          "success": false
        }
        ```

        #### D-997 (EXPIRED_TOKEN_ERROR) - RefreshToken 만료
        - **원인**: RefreshToken의 유효기간이 만료됨
        - **대응**: 재로그인 필요 → `/api/auth/login` 호출
        ```json
        {
          "code": "D-997",
          "message": "토큰이 만료되었습니다.",
          "success": false
        }
        ```

        ### 클라이언트 구현 예시
        ```kotlin
        // 1. API 호출 시 401 에러 발생
        // 2. 에러 코드 확인 후 분기 처리
        when (errorCode) {
            "D-997" -> {
                // AccessToken 만료 → RefreshToken으로 갱신
                val newTokens = authApi.refreshToken(storedRefreshToken)
                // 새로운 토큰 저장
                saveTokens(newTokens.accessToken, newTokens.refreshToken)
                // 원래 요청 재시도
                retryOriginalRequest()
            }
            "D-998", "D-999" -> {
                // 토큰 무효 또는 인증 실패 → 재로그인
                navigateToLogin()
            }
        }
        ```

        ### 주의사항
        - RefreshToken도 만료될 수 있으므로, 만료 시 재로그인 처리가 필요합니다.
        - 보안을 위해 RefreshToken은 안전한 저장소에 보관하세요.
        - 동시에 여러 번 갱신 요청을 보내지 마세요 (Rotation 정책으로 인해 실패할 수 있음).
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "토큰 갱신 성공 - 새로운 AccessToken과 RefreshToken 발급"),
        ApiResponse(
            responseCode = "401",
            description = "D-997: RefreshToken 만료 (재로그인 필요) / D-998: RefreshToken 무효 (재로그인 필요)",
        ),
    )
    @PostMapping("/refresh")
    fun refreshToken(@RequestBody @Valid request: RefreshTokenRequest): BaseResponse<GetTokenResponse> {
        val result = refreshTokenUseCase.execute(RefreshTokenCommand(refreshToken = request.refreshToken))

        return BaseResponse(data = GetTokenResponse(accessToken = result.accessToken, result.refreshToken))
    }

    /**
     * ****** Test용이므로 Swagger Hidden 처리 ******
     * 테스트용 카카오 OAuth Redirect 엔드포인트
     * Authorization Code를 받아서 idToken으로 교환
     */
    @Hidden
    @GetMapping("/test/kakao/redirect")
    fun kakaoTestRedirect(@RequestParam code: String): BaseResponse<GetKakaoTokenResponse> {
        val tokenResponse = kakaoRegisterUseCase.getAccessTokenByCode(code)
        return BaseResponse(
            data = GetKakaoTokenResponse(
                accessToken = tokenResponse.accessToken,
                tokenType = tokenResponse.tokenType,
                refreshToken = tokenResponse.refreshToken,
                expiresIn = tokenResponse.expiresIn,
                scope = tokenResponse.scope,
                refreshTokenExpiresIn = tokenResponse.refreshTokenExpiresIn,
                idToken = tokenResponse.idToken,
            ),
        )
    }
}
