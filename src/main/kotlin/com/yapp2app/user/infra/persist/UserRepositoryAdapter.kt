package com.yapp2app.user.infra.persist

import com.yapp2app.user.application.port.UserRepositoryPort
import com.yapp2app.user.domain.entity.User
import com.yapp2app.user.domain.enums.ProviderType
import com.yapp2app.user.infra.persist.jpa.UserRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : UserRepositoryAdapter
 * author         : darren
 * date           : 2025. 12. 29. 14:05
 * description    : User 영속성에 대한 Adapter (command + query)
 */
@Repository
class UserRepositoryAdapter(private val jpaRepository: UserRepository) : UserRepositoryPort {

    override fun save(user: User): User = jpaRepository.save(user)

    override fun findByOid(oid: String, providerType: ProviderType): User? =
        jpaRepository.findByOidAndProviderType(oid, providerType)
}
