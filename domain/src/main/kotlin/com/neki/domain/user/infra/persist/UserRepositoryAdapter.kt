package com.neki.domain.user.infra.persist

import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.User
import com.neki.domain.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * fileName       : UserRepositoryAdapter
 * author         : darren
 * date           : 2025. 12. 29. 14:05
 * description    : User 영속성에 대한 Adapter (command + query)
 */
@Repository
class UserRepositoryAdapter(private val jpaRepository: JpaUserRepository) : UserRepository {

    override fun save(user: User): User = jpaRepository.save(user)

    override fun findByOid(oid: String, providerType: ProviderType): User? =
        jpaRepository.findByOidAndProviderType(oid, providerType)

    override fun findById(id: Long): User? = jpaRepository.findByIdOrNull(id)

    override fun countByOidIsNotNull(): Long = jpaRepository.countByOidIsNotNull()
}
