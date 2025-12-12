package com.yapp2app.user.api.controller

import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.user.api.dto.KakaoOIDCLoginRequest
import com.yapp2app.user.api.dto.TokenResponse
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
@RequestMapping("/api/auth")
@RestController
class AuthController {

    /**
     * OIDC 방식 로그인
     */
    @PostMapping("/kakao/oidc")
    fun kakaoLoginWithOIDC(
        @RequestBody request: KakaoOIDCLoginRequest
    ): BaseResponse<TokenResponse> {
        return BaseResponse(data=TokenResponse("OK", "OK"))
    }
}