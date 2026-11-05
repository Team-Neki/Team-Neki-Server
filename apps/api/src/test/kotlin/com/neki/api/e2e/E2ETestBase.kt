package com.neki.api.e2e

import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import com.neki.domain.user.infra.security.token.AuthTokenProviderAdapter
import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

/**
 * fileName       : E2ETestBase
 * author         : koo
 * date           : 2025. 12. 28. 오후 10:50
 * description    : E2E test 편의를 Base class, 공통 기능을 상속하여 사용
 */
abstract class E2ETestBase {

    @Autowired
    protected lateinit var tokenProvider: AuthTokenProviderAdapter

    @Autowired
    protected lateinit var userRepository: JpaUserRepository

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    /** V34의 COLLECTED 제약에 맞춰 JPA가 매핑하지 않는 수집 원본 필드를 채운다. */
    protected fun markCollectedLocation(id: Long, platform: String, idx: String) {
        jdbcTemplate.update(
            """
            UPDATE tb_photo_booth_location
            SET source_type = 'COLLECTED',
                source_platform = ?,
                source_idx = ?,
                source_name = branch_name,
                source_address = address,
                source_location = location,
                source_dt = CURRENT_DATE,
                source_collected_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """.trimIndent(),
            platform,
            idx,
            id,
        )
    }

    /**
     * 모든 테스트 컨텍스트가 같은 postgis 컨테이너(TC_DAEMON)를 쓰므로, 매 테스트 전에 데이터를 전부 비운다.
     * Flyway 초기 데이터(V3 브랜드, V5 앱 버전, V7/V19 약관 등)와 다른 테스트 클래스가 남긴 행이
     * 실행 순서에 따라 unique 충돌이나 개수 검증 실패를 일으키지 않게 한다.
     * 하위 클래스의 @BeforeEach 보다 먼저 실행된다.
     */
    @BeforeEach
    protected fun truncateAllTables() {
        val tables: List<String> = jdbcTemplate.queryForList(
            """
            SELECT quote_ident(tablename)
            FROM pg_tables
            WHERE schemaname = 'public'
              AND tablename NOT IN ('flyway_schema_history', 'spatial_ref_sys')
            """.trimIndent(),
            String::class.java,
        )
        if (tables.isNotEmpty()) {
            jdbcTemplate.execute("TRUNCATE TABLE ${tables.joinToString()} CASCADE")
        }
    }

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
