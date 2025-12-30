package com.yapp2app.auth.api.controller

import com.yapp2app.auth.api.request.KakaoRegisterRequest
import com.yapp2app.auth.api.request.LoginRequest
import com.yapp2app.auth.api.response.GetKakaoRegisterResponse
import com.yapp2app.auth.api.response.GetKakaoTokenResponse
import com.yapp2app.auth.api.response.GetTokenResponse
import com.yapp2app.auth.application.command.LoginCommand
import com.yapp2app.auth.application.command.RegisterKakaoUserCommand
import com.yapp2app.auth.application.usecase.KakaoRegisterUseCase
import com.yapp2app.auth.application.usecase.LoginUseCase
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
class AuthController(private val kakaoRegisterUseCase: KakaoRegisterUseCase, private val loginUseCase: LoginUseCase) {

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

    fun login(@RequestBody @Valid request: LoginRequest): BaseResponse<GetTokenResponse> {
        val result = loginUseCase.execute(LoginCommand(oid = request.oid, providerType = request.providerType))

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
