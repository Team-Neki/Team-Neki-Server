package com.neki.admin.auth.api

import com.neki.admin.auth.application.AdminAccountService
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.user.models.User
import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/v1/auth")
class AdminAuthController(private val accounts: AdminAccountService) {
    @Operation(summary = "현재 로그인 사용자와 ADMIN 권한 및 CSRF 토큰 조회")
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal(expression = "userId") userId: Long,
        @AuthenticationPrincipal(expression = "subject") subject: String,
        csrfToken: CsrfToken,
    ): BaseResponse<AdminAuthResponse> {
        val user: User = accounts.findActive(userId, subject)
            ?: throw BadCredentialsException("사용할 수 없는 계정입니다.")
        return BaseResponse(
            data = AdminAuthResponse(
                userId = user.id!!,
                email = user.email,
                name = user.name,
                admin = accounts.isAdmin(user),
                csrfToken = csrfToken.token,
                csrfHeaderName = csrfToken.headerName,
            ),
        )
    }
}

data class AdminAuthResponse(
    val userId: Long,
    val email: String?,
    val name: String?,
    val admin: Boolean,
    val csrfToken: String,
    val csrfHeaderName: String,
)
