package com.neki.user.infra.persist.jpa

import com.neki.user.models.ProviderType
import com.neki.user.models.User
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : UserRepository
 * author         : darren
 * date           : 2025. 12. 18. 18:45
 * description    : User Entity Repository
 */
interface JpaUserRepository : JpaRepository<User, Long> {

    fun existsByName(name: String): Boolean

    fun findByOidAndProviderType(oid: String, providerType: ProviderType): User?

    fun countByOidIsNotNull(): Long
}
