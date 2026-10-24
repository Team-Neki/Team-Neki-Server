package com.neki.admin.auth

import com.neki.admin.auth.application.AdminAccountFacade
import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import com.neki.domain.user.models.RoleType
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Import
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AdminAuthTestController::class)
class AdminAuthorizationDatabaseErrorTest {
    @Autowired private lateinit var mvc: MockMvc

    @Autowired private lateinit var users: JpaUserRepository

    @Autowired private lateinit var sessions: JdbcIndexedSessionRepository

    @SpyBean private lateinit var accounts: AdminAccountFacade

    @Test
    fun databaseFailureDuringAuthorizationReturns503() {
        val user = AdminTestSession.createUser(users, RoleType.ADMIN.role)
        val login = AdminTestSession.login(sessions, user)
        doThrow(DataRetrievalFailureException("database unavailable"))
            .`when`(accounts).isAdmin(UserCommand.AdminAccountAccess(user.id!!, user.oid!!))

        mvc.perform(get("/admin/v1/test").cookie(login.cookie))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.resultCode").value("D-99"))
    }
}
