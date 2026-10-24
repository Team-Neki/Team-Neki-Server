package com.neki.admin

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource

/** 전체 운영 마이그레이션을 적용한 새 PostGIS DB에서 admin의 Flyway/Hibernate 검증을 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("prod")
@TestPropertySource(
    properties = [
        "GOOGLE_CLIENT_ID=admin-schema-test",
        "GOOGLE_CLIENT_SECRET=admin-schema-test-secret",
        "JASYPT_PASSWORD=admin-schema-test-key",
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

    @Test
    fun contextLoads() = Unit
}
