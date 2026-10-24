package com.neki.admin.auth.application

import com.neki.admin.auth.application.dto.AdminAccountResult
import com.neki.core.annotation.UseCase
import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.dto.UserQuery
import com.neki.domain.user.models.AdminIdentity
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import com.neki.domain.user.service.AdminAccountAccess
import com.neki.domain.user.service.AdminAccountService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminAccountFacade(private val accounts: AdminAccountService) : AdminAccountAccess {
    // 저장 실패 후 새 조회가 가능하도록 각 repository 호출의 트랜잭션을 사용한다.
    override fun findOrCreate(identity: AdminIdentity): Long {
        val query = UserQuery.GetAdminAccount(identity.key)
        accounts.findAdminAccount(query)?.let { return it.id!! }
        return try {
            accounts.registerAdminAccount(UserCommand.RegisterAdminAccount(identity)).id!!
        } catch (exception: DataIntegrityViolationException) {
            accounts.findAdminAccount(query)?.id ?: throw exception
        }
    }

    @Transactional(readOnly = true)
    fun findActive(userId: Long, identityKey: String): AdminAccountResult? {
        val user: User = accounts.findActiveAdminAccount(UserQuery.GetActiveAdminAccount(userId, identityKey))
            ?: return null
        return AdminAccountResult(
            userId = user.id!!,
            email = user.email,
            name = user.name,
            admin = user.hasRole(RoleType.ADMIN),
        )
    }

    @Transactional(readOnly = true)
    override fun isAdmin(userId: Long, identityKey: String): Boolean = findActive(userId, identityKey)?.admin == true
}
