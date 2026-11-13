package com.neki.admin.auth.api.dto

import com.neki.admin.auth.application.dto.AdminAccountResult
import com.neki.domain.user.dto.UserQuery

data class AdminAuthResponse(val userId: Long, val email: String?, val name: String?, val admin: Boolean)

object AdminAuthConverter {
    fun toQuery(userId: Long): UserQuery.GetActiveAdminAccount = UserQuery.GetActiveAdminAccount(userId)

    fun from(account: AdminAccountResult): AdminAuthResponse = AdminAuthResponse(
        userId = account.userId,
        email = account.email,
        name = account.name,
        admin = account.admin,
    )
}
