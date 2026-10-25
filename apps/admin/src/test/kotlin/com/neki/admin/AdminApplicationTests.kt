package com.neki.admin

import com.neki.admin.common.exception.handler.ExceptionHandler
import io.mockk.mockk
import io.mockk.verify
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class AdminApplicationTests {

    @Autowired
    lateinit var flywayMigrationStrategy: FlywayMigrationStrategy

    @Test
    fun contextLoads() {
    }

    @Test
    fun validatesSharedSchemaWithoutApplyingMigrations() {
        val flyway = mockk<Flyway>(relaxed = true)

        flywayMigrationStrategy.migrate(flyway)

        verify(exactly = 1) { flyway.validate() }
        verify(exactly = 0) { flyway.migrate() }
    }

    @Test
    fun unexpectedExceptionReturnsServerError() {
        val response = ExceptionHandler().exceptionHandler(IllegalStateException("unexpected"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }
}
