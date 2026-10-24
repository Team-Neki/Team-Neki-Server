package com.neki.domain.user.service

import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.dto.UserQuery
import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import com.neki.domain.user.repository.UserRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "admin.auth", name = ["enabled"], havingValue = "true")
class AdminAccountService(private val userRepository: UserRepository) {
    fun findAdminAccount(query: UserQuery.GetAdminAccount): User? =
        userRepository.findByOid(query.identityKey, ProviderType.ADMIN_SSO)

    fun registerAdminAccount(command: UserCommand.RegisterAdminAccount): User {
        val points: IntArray = (command.name?.takeIf { it.isNotBlank() } ?: "소셜 사용자")
            .codePoints().limit(10).toArray()
        return userRepository.save(
            User(
                email = command.email,
                name = String(points, 0, points.size),
                oid = command.identityKey,
                roles = RoleType.USER.role,
                providerType = ProviderType.ADMIN_SSO,
                profileImageId = null,
            ),
        )
    }

    fun findActiveAdminAccount(query: UserQuery.GetActiveAdminAccount): User? = userRepository.findById(query.userId)
        ?.takeIf { it.providerType == ProviderType.ADMIN_SSO && it.oid == query.identityKey }
}
