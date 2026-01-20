package com.yapp2app.user.application.port

import com.yapp2app.user.domain.entity.User
import com.yapp2app.user.domain.enums.ProviderType

/**
 * fileName       : UserRepositoryPort
 * author         : darren
 * date           : 2025. 12. 29. 14:07
 * description    : User 영속성 관련 포트 (command + query)
 */
interface UserRepositoryPort {
    fun save(user: User): User

    fun findByOid(oid: String, provider: ProviderType): User?
}
