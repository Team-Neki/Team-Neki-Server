package com.yapp2app.auth.api.controller

import com.yapp2app.auth.api.converter.AuthCommandConverter
import com.yapp2app.auth.api.converter.AuthResultConverter
import com.yapp2app.auth.api.dto.CreateAuthRequest
import com.yapp2app.auth.api.dto.GetAuthResponse
import com.yapp2app.auth.api.dto.GetKakaoTokenResponse
import com.yapp2app.auth.api.dto.RefreshTokenRequest
import com.yapp2app.auth.application.usecase.OauthLoginUseCase
import com.yapp2app.auth.application.usecase.RefreshTokenUseCase
import com.yapp2app.common.api.dto.BaseResponse
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
    private val oauthLoginUseCase: OauthLoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val commandConverter: AuthCommandConverter,
    private val resultConverter: AuthResultConverter,
) {

    /**
     * OIDC 방식 회원가입
     */
    @Operation(
        summary = "카카오/애플 OIDC 회원가입",
        description = """
        ## 카카오/애플 OIDC 회원가입 API

        [========== 카카오 REST 방식 구간(Start) ==========]

        (SDK 방식은 안읽으셔도 되요)

        앱에서 카카오 SDK로 획득한 idToken을 검증하고 회원가입을 처리합니다.

        ### 테스트용 idToken 발급 방법 (AOS, IOS 는 SDK로 부터 얻은 idToken 바로 넣어주면 됩니다!)

        #### 1단계: Authorization Code 획득
        아래 URL을 브라우저에서 실행하여 카카오 로그인 후 idToken 얻습니다.

        [local] https://kauth.kakao.com/oauth/authorize?client_id=4db94315d17162e99b36029f6f9775c6&redirect_uri=http://localhost:8080/api/auth/test/kakao/redirect&response_type=code&scope=openid,profile_nickname,profile_image

        [staging] https://kauth.kakao.com/oauth/authorize?client_id=4db94315d17162e99b36029f6f9775c6&redirect_uri=https://dev-yapp.suitestudy.com:4641/api/auth/test/kakao/redirect&response_type=code&scope=openid,profile_nickname,profile_image

        응답의 `id_token` 필드 값을 이 API의 `idToken`으로 사용하세요.

        [========== 카카오 REST 방식 구간(End) ==========]

        사용자의 OID와 ProviderType을 사용하여 로그인을 수행합니다.

        ### 성공 응답 (200 OK)
        - **accessToken**: API 요청에 사용할 액세스 토큰 (유효기간: 설정값에 따름)
        - **refreshToken**: 액세스 토큰 갱신에 사용할 리프레시 토큰 (유효기간: 설정값에 따름)

        ### API 호출 시 토큰 사용법
        ```
        Authorization: Bearer {accessToken}
        ```

        ### 토큰 만료 시 처리 방법
        1. 인가가 필요한 API 호출 시 **[HttpStatus] 401 Unauthorized** 응답을 받은 경우 -> 토큰 갱신 API 호출
        2. **HttpStatus 403 Forbidden** 응답을 받은 경우 -> 로그인 페이지로 리다이렉트

        ### 토큰 저장 권장사항
        - **accessToken**: 메모리 또는 안전한 저장소 (탈취 위험 최소화)
        - **refreshToken**: 안전한 저장소 (Keychain, EncryptedSharedPreferences 등)

        인가가 필요한 API 요청
           ↓
        응답 수신
           ↓
        ┌──────────────┐
        │ Status Code? │
        └──────────────┘
           ↓
           ├─ 200 → 정상 처리
           │
           ├─ 403 → 로그인 페이지 이동
           │
           ├─ 401 → resultCode 확인
           │
           └─ 400 → message를 모달로 표시

        providerType LOCAL, TEST는 무시해주세요 ~
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "카카오 OIDC 엔드포인트가 정상적으로 작동합니다."),
    )
    @PostMapping("/{providerType}/login")
    fun oauthLogin(
        @Parameter(description = "로그인 제공자 타입 (kakao, apple)", example = "kakao")
        @PathVariable(name = "providerType") providerType: String,
        @RequestBody @Valid request: CreateAuthRequest,
    ): BaseResponse<GetAuthResponse> {
        val command = commandConverter.toCreateAuthCommand(request, providerType)

        val result = oauthLoginUseCase.execute(command)

        val response = resultConverter.toCreateAuthResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "토큰 갱신",
        description = """
        ## AccessToken 갱신 API

        RefreshToken을 사용하여 새로운 AccessToken과 RefreshToken을 발급받습니다.

        ### 사용 시나리오
        1. 인가가 필요한 API 호출 시 **401 Unauthorized** 응답을 받음
        2. 저장된 RefreshToken으로 이 API를 호출하여 새로운 토큰 발급

        ### Refresh Token Rotation (보안 강화)
        ⚠️ **중요**: 보안을 위해 Refresh Token Rotation을 적용합니다.
        - 새로운 **AccessToken**과 함께 새로운 **RefreshToken**도 함께 발급됩니다.
        - 기존 RefreshToken은 반드시 새로운 RefreshToken을 저장해야 합니다.

        [만료된 RefreshToken] eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sIm5hbWUiOiLrjIDtmIQiLCJwcm92aWRlcl90eXBlIjoiS0FLQU8iLCJpYXQiOjE3NjczMzQ2MDAsImV4cCI6MTc2NzMzNTIwMH0.Q1IQrm0QC30GJHA0bQj3TygdoveCiWbzRJ3ig9sDRlwaZqF-HNyshf9Jab8k1C9Ec3RerxJhzgHWEV-Ap7lecw

        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "토큰 갱신 성공 - 새로운 AccessToken과 RefreshToken 발급"),
        ApiResponse(
            responseCode = "400",
            description = "D-998: RefreshToken 만료 (재로그인 필요 HttpStatus 403 반환) 로그인 페이지로 이동",
        ),
    )
    @PostMapping("/refresh")
    fun refreshToken(@RequestBody @Valid request: RefreshTokenRequest): BaseResponse<GetAuthResponse> {
        val command = commandConverter.toRefreshTokenCommand(request)

        val result = refreshTokenUseCase.execute(command)

        val response = resultConverter.toCreateAuthResponse(result)

        return BaseResponse(data = response)
    }

    /**
     * ****** Test용이므로 Swagger Hidden 처리 ******
     * 테스트용 카카오 OAuth Redirect 엔드포인트
     * Authorization Code를 받아서 idToken으로 교환
     */
    @Hidden
    @GetMapping("/test/kakao/redirect")
    fun kakaoTestRedirect(@RequestParam code: String): BaseResponse<GetKakaoTokenResponse> {
        val tokenResponse = oauthLoginUseCase.getAccessTokenByCode(code)
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
