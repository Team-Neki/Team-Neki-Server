package com.neki.admin.auth.application

import com.neki.admin.auth.application.dto.AdminAccountResult
import com.neki.core.annotation.UseCase
import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.dto.UserQuery
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import com.neki.domain.user.service.AdminAccountService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminAccountFacade(private val accounts: AdminAccountService) {
    // 저장 실패 후 새 조회가 가능하도록 각 repository 호출의 트랜잭션을 사용한다.
    fun findOrCreate(subject: String, email: String, name: String?): Long {
        val query = UserQuery.GetGoogleAccount(subject)
        accounts.findGoogleAccount(query)?.let { return it.id!! }
        return try {
            accounts.registerGoogleAccount(UserCommand.RegisterGoogleAccount(subject, email, name)).id!!
        } catch (exception: DataIntegrityViolationException) {
            accounts.findGoogleAccount(query)?.id ?: throw exception
        }
    }

    @Transactional(readOnly = true)
    fun findActive(userId: Long, subject: String): AdminAccountResult? {
        val user: User = accounts.findActiveGoogleAccount(UserQuery.GetActiveGoogleAccount(userId, subject))
            ?: return null
        return AdminAccountResult(
            userId = user.id!!,
            email = user.email,
            name = user.name,
            admin = user.hasRole(RoleType.ADMIN),
        )
    }
}
