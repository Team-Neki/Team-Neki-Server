package com.neki.admin

import com.neki.admin.auth.application.AdminAccountFacade
import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 전체 운영 마이그레이션을 적용한 새 PostGIS DB에서 admin의 Flyway/Hibernate 검증과 PostgreSQL 전용 제약을 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("prod")
@TestPropertySource(
    properties = [
        "GOOGLE_CLIENT_ID=admin-schema-test",
        "GOOGLE_CLIENT_SECRET=admin-schema-test-secret",
        "JASYPT_PASSWORD=admin-schema-test-key",
        "aws.s3.access-key=admin-schema-test",
        "aws.s3.secret-key=admin-schema-test-secret",
        "aws.s3.bucket=admin-schema-test",
        "spring.flyway.locations=classpath:db/migration,classpath:db/admin-test-migration",
        "spring.flyway.baseline-on-migrate=true",
    ],
)
class AdminProductionSchemaTest {
    companion object {
        private const val JDBC_URL = "jdbc:tc:postgis:14-3.4:///neki?TC_DAEMON=true"

        init {
            Flyway.configure()
                .dataSource(JDBC_URL, "test", "test")
                .locations("classpath:db/migration", "classpath:db/admin-test-migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate()
        }

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { JDBC_URL }
            registry.add("spring.datasource.driver-class-name") { "org.testcontainers.jdbc.ContainerDatabaseDriver" }
            registry.add("spring.datasource.username") { "test" }
            registry.add("spring.datasource.password") { "test" }
        }
    }

    @Autowired
    private lateinit var accountFacade: AdminAccountFacade

    @Autowired
    private lateinit var users: JpaUserRepository

    @Test
    fun contextLoads() = Unit

    @Test
    @DisplayName("같은 이메일로 동시에 최초 로그인해도 V44 partial unique 제약으로 관리 계정은 하나만 생성된다")
    fun givenConcurrentFirstLogins_whenSameEmail_thenOneAccount() {
        val email = "concurrent-${UUID.randomUUID()}@example.com"
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = (1..8).map {
                executor.submit<Long> {
                    start.await()
                    accountFacade.findOrCreate(UserCommand.RegisterAdminAccount(email, "동시 로그인"))
                }
            }
            start.countDown()
            val ids: List<Long> = futures.map { it.get(20, TimeUnit.SECONDS) }
            assertThat(ids.toSet()).hasSize(1)
            assertThat(users.findAll().count { it.email == email }).isEqualTo(1)
        } finally {
            executor.shutdownNow()
        }
    }
}
