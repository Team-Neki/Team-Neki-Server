package com.neki.user.repository

import com.neki.user.models.ProviderType
import com.neki.user.models.User

/**
 * fileName       : UserRepository
 * author         : darren
 * date           : 2025. 12. 29. 14:07
 * description    : User 영속성 관련 포트 (command + query)
 */
interface UserRepository {
    fun save(user: User): User

    fun findByOid(oid: String, provider: ProviderType): User?

    fun findById(id: Long): User?

    fun countByOidIsNotNull(): Long
}
