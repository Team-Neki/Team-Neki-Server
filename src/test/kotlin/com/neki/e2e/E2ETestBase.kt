package com.neki.e2e

import com.neki.auth.infra.security.token.AuthTokenProvider
import com.neki.user.domain.entity.User
import com.neki.user.domain.enums.ProviderType
import com.neki.user.domain.enums.RoleType
import com.neki.user.infra.persist.jpa.UserRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired

/**
 * fileName       : E2ETestBase
 * author         : koo
 * date           : 2025. 12. 28. 오후 10:50
 * description    : E2E test 편의를 Base class, 공통 기능을 상속하여 사용
 */
abstract class E2ETestBase {

    @Autowired
    protected lateinit var tokenProvider: AuthTokenProvider

    @Autowired
    protected lateinit var userRepository: UserRepository

    @AfterEach
    protected open fun tearDown() {
        userRepository.deleteAllInBatch()
    }

    fun createTestUserAndToken(
        email: String = "test-${System.currentTimeMillis()}@example.com",
        name: String = "테스트 사용자",
        password: String = "Test1234!",
        providerType: ProviderType = ProviderType.TEST,
        roles: String = "${RoleType.USER.role},${RoleType.ADMIN.role}",
    ): Pair<User, String> {
        val user = userRepository.save(
            createUser(
                email,
                name,
                password,
                providerType,
                roles,
            ),
        )

        val token = tokenProvider.createAccessToken(
            id = user.id.toString(),
            name = user.name,
            roles = user.roles.split(","),
            providerType = user.providerType,
        )

        return user to token
    }

    private fun createUser(
        email: String,
        name: String,
        password: String,
        providerType: ProviderType,
        roles: String,
    ): User = User(
        email = email,
        name = name,
        password = password,
        oid = System.currentTimeMillis().toString(),
        providerType = providerType,
        roles = roles,
        profileImageId = null,
    )
}
