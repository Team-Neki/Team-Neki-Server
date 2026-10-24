package com.neki.admin.auth.api.dto

import com.neki.admin.auth.application.dto.AdminAccountResult
import org.springframework.security.web.csrf.CsrfToken

data class AdminAuthResponse(
    val userId: Long,
    val email: String?,
    val name: String?,
    val admin: Boolean,
    val csrfToken: String,
    val csrfHeaderName: String,
)

object AdminAuthConverter {
    fun from(account: AdminAccountResult, csrfToken: CsrfToken): AdminAuthResponse = AdminAuthResponse(
        userId = account.userId,
        email = account.email,
        name = account.name,
        admin = account.admin,
        csrfToken = csrfToken.token,
        csrfHeaderName = csrfToken.headerName,
    )
}
