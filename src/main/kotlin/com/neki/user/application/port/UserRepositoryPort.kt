package com.neki.user.application.port

import com.neki.user.domain.entity.User
import com.neki.user.domain.enums.ProviderType

/**
 * fileName       : UserRepositoryPort
 * author         : darren
 * date           : 2025. 12. 29. 14:07
 * description    : User 영속성 관련 포트 (command + query)
 */
interface UserRepositoryPort {
    fun save(user: User): User

    fun findByOid(oid: String, provider: ProviderType): User?

    fun findById(id: Long): User?
}
