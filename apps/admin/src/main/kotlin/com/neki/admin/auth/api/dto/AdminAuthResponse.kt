package com.neki.admin.auth.api.dto

import com.neki.admin.auth.application.dto.AdminAccountResult

data class AdminAuthResponse(val userId: Long, val email: String?, val name: String?, val admin: Boolean)

object AdminAuthConverter {
    fun from(account: AdminAccountResult): AdminAuthResponse = AdminAuthResponse(
        userId = account.userId,
        email = account.email,
        name = account.name,
        admin = account.admin,
    )
}
