package com.yapp2app.user.application.repository

import com.yapp2app.user.domain.entity.User
import com.yapp2app.user.domain.enums.ProviderType
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : UserRepository
 * author         : darren
 * date           : 2025. 12. 18. 18:45
 * description    : User Entity Repository
 */
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmailAndProviderType(email: String, providerType: ProviderType): User?

    fun existsByEmailAndProviderType(email: String, providerType: ProviderType): Boolean
}
