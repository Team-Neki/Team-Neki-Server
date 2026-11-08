package com.neki.admin.auth.api.controller

import com.neki.admin.auth.api.dto.AdminAuthConverter
import com.neki.admin.auth.api.dto.AdminAuthResponse
import com.neki.admin.auth.application.AdminAccountFacade
import com.neki.admin.auth.application.dto.AdminAccountResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.user.dto.UserCommand
import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/v1/auth")
class AdminAuthController(private val accounts: AdminAccountFacade) {
    @Operation(summary = "현재 로그인 사용자와 ADMIN 권한 조회")
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal(expression = "userId") userId: Long,
        @AuthenticationPrincipal(expression = "identityKey") identityKey: String,
    ): BaseResponse<AdminAuthResponse> {
        val user: AdminAccountResult = accounts.findActive(UserCommand.AdminAccountAccess(userId, identityKey))
            ?: throw BadCredentialsException("사용할 수 없는 계정입니다.")
        return BaseResponse(
            data = AdminAuthConverter.from(user),
        )
    }
}
