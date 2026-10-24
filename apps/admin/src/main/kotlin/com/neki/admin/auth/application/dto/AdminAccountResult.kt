package com.neki.admin.auth.application.dto

import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User

data class AdminAccountResult(val userId: Long, val email: String?, val name: String?, val admin: Boolean)

object AdminAccountAssembler {
    fun from(user: User): AdminAccountResult = AdminAccountResult(
        userId = user.id!!,
        email = user.email,
        name = user.name,
        admin = user.hasRole(RoleType.ADMIN),
    )
}
