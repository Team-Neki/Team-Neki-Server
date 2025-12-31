package com.yapp2app.user.api

import com.yapp2app.auth.infra.security.token.UserPrincipal
import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.user.api.response.GetUserInfoResponse
import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : UserController
 * author         : koo
 * date           : 2025. 12. 28. 오후 7:39
 * description    :
 */
@RestController
@RequestMapping("/api/users")
class UserController {

    @Operation(
        summary = "내 정보 조회",
        description = """
        AccessToken 만료 시 HttpStatus 401

        """,
    )
    @RequiresSecurity // ← Swagger UI에서 JWT 토큰 전송
    @GetMapping("/info")
    fun info(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ): BaseResponse<GetUserInfoResponse> = BaseResponse(
        data = GetUserInfoResponse(
            name = userPrincipal.name!!,
            profileImageUrl = "temp",
            providerType = userPrincipal.providerType,
        ),
    )
}
