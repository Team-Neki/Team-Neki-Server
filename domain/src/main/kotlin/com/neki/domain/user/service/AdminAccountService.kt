package com.neki.domain.user.service

import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.dto.UserQuery
import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import com.neki.domain.user.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class AdminAccountService(private val userRepository: UserRepository) {
    fun findGoogleAccount(query: UserQuery.GetGoogleAccount): User? =
        userRepository.findByOid(query.subject, ProviderType.GOOGLE)

    fun registerGoogleAccount(command: UserCommand.RegisterGoogleAccount): User {
        val points: IntArray = (command.name?.takeIf { it.isNotBlank() } ?: "Google 사용자")
            .codePoints().limit(10).toArray()
        return userRepository.save(
            User(
                email = command.email,
                name = String(points, 0, points.size),
                oid = command.subject,
                roles = RoleType.USER.role,
                providerType = ProviderType.GOOGLE,
                profileImageId = null,
            ),
        )
    }

    fun findActiveGoogleAccount(query: UserQuery.GetActiveGoogleAccount): User? = userRepository.findById(query.userId)
        ?.takeIf { it.providerType == ProviderType.GOOGLE && it.oid == query.subject }
}
