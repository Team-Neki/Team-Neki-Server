package com.neki.admin.auth.application

import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import com.neki.domain.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAccountService(private val userRepository: UserRepository) {
    // 각 repository 호출의 트랜잭션을 사용한다. 동시 최초 로그인 충돌 후에는 새 조회로 복구한다.
    fun findOrCreate(subject: String, email: String, name: String?): User {
        userRepository.findByOid(subject, ProviderType.GOOGLE)?.let { return it }
        val points: IntArray = (name?.takeIf { it.isNotBlank() } ?: "Google 사용자")
            .codePoints().limit(10).toArray()
        val user = User(
            email = email,
            name = String(points, 0, points.size),
            oid = subject,
            roles = RoleType.USER.role,
            providerType = ProviderType.GOOGLE,
            profileImageId = null,
        )
        return try {
            userRepository.save(user)
        } catch (exception: DataIntegrityViolationException) {
            userRepository.findByOid(subject, ProviderType.GOOGLE) ?: throw exception
        }
    }

    @Transactional(readOnly = true)
    fun findActive(userId: Long, subject: String): User? = userRepository.findById(userId)
        ?.takeIf { it.providerType == ProviderType.GOOGLE && it.oid == subject }

    fun isAdmin(user: User): Boolean = user.roles.split(',').any { it.trim() == RoleType.ADMIN.role }
}
