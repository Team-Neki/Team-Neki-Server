package com.neki.admin.auth.application

import com.neki.admin.auth.application.dto.AdminAccountResult
import com.neki.domain.user.dto.UserQuery
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import com.neki.domain.user.service.AdminAccountService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAccountFacade(private val accounts: AdminAccountService) {
    @Transactional(readOnly = true)
    fun findActive(query: UserQuery.GetActiveAdminAccount): AdminAccountResult? {
        val user: User = accounts.findActiveAdminAccount(query) ?: return null
        return AdminAccountResult(
            userId = user.id!!,
            email = user.email,
            name = user.name,
            admin = user.hasRole(RoleType.ADMIN),
        )
    }
}
