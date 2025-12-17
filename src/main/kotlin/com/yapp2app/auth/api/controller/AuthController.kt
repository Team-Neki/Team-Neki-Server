package com.yapp2app.auth.api.controller

import com.yapp2app.auth.api.dto.KakaoOIDCLoginRequest
import com.yapp2app.auth.api.dto.TokenResponse
import com.yapp2app.common.api.dto.BaseResponse
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : AuthController
 * author         : darren
 * date           : 2025. 12. 12. 13:18
 * description    :
 */
@Tag(name = "AuthController", description = "인증/인가 API")
@RequestMapping("/api/auth")
@RestController
class AuthController {

    /**
     * OIDC 방식 로그인
     */
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "카카오 OIDC 엔드포인트가 정상적으로 작동합니다."),
    )
    @PostMapping("/kakao/oidc")
    fun kakaoLoginWithOIDC(@RequestBody request: KakaoOIDCLoginRequest): BaseResponse<TokenResponse> =
        BaseResponse(data = TokenResponse("OK", "OK"))
}
