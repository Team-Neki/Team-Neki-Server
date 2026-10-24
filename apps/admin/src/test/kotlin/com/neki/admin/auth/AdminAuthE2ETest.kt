package com.neki.admin.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.admin.auth.application.AdminAccountFacade
import com.neki.admin.auth.infra.security.AdminPrincipal
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestComponent
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AdminAuthTestController::class)
@DisplayName("어드민 세션 인증과 현재 DB 권한 인가")
class AdminAuthE2ETest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var users: JpaUserRepository

    @Autowired
    private lateinit var jdbcSessions: JdbcIndexedSessionRepository

    @Suppress("UNCHECKED_CAST")
    private val sessions: SessionRepository<Session>
        get() = jdbcSessions as SessionRepository<Session>

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var routes: AdminAuthTestController

    @Autowired
    private lateinit var accounts: AdminAccountFacade

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM ADMIN_SESSION_ATTRIBUTES")
        jdbc.update("DELETE FROM ADMIN_SESSION")
        users.deleteAllInBatch()
        routes.writes.set(0)
    }

    @Test
    @DisplayName("익명 API 요청은 로그인 화면 이동 대신 401을 반환한다")
    fun givenAnonymous_whenRequestingMeOrAdmin_thenUnauthorized() {
        mvc.perform(get("/admin/v1/auth/me")).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("D-999"))
        mvc.perform(get("/admin/v1/test")).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("대기 사용자는 JDBC에 저장된 세션으로 본인과 CSRF를 조회하지만 관리 API는 거부된다")
    fun givenPendingUser_whenRequestingMe_thenIdentityAndCsrfWithoutAdminAccess() {
        val user: User = createUser()
        val login = login(user)
        val me: JsonNode = me(login.cookie)

        assertThat(me["userId"].asLong()).isEqualTo(user.id)
        assertThat(me["email"].asText()).isEqualTo(user.email)
        assertThat(me["admin"].asBoolean()).isFalse()
        assertThat(me.has("csrfToken")).isFalse()
        assertThat(csrf(login.cookie)).isNotBlank()
        mvc.perform(get("/admin/v1/test").cookie(login.cookie)).andExpect(status().isForbidden)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ADMIN_SESSION_ATTRIBUTES", Int::class.java))
            .isGreaterThan(0)
        assertThat(sessions.findById(login.id)!!.maxInactiveInterval).isEqualTo(Duration.ofMinutes(30))
    }

    @Test
    @DisplayName("DB에서 ADMIN을 부여하면 재로그인 없이 동일 세션의 다음 요청부터 허용한다")
    fun givenPendingSession_whenAdminGranted_thenSameSessionImmediatelyAllowed() {
        val user: User = createUser()
        val login = login(user)
        val csrf: String = csrf(login.cookie)
        mvc.perform(get("/admin/v1/test").cookie(login.cookie)).andExpect(status().isForbidden)

        user.roles = "${RoleType.USER.role},${RoleType.ADMIN.role}"
        users.saveAndFlush(user)

        mvc.perform(get("/admin/v1/test").cookie(login.cookie)).andExpect(status().isOk)
        assertThat(me(login.cookie)["admin"].asBoolean()).isTrue()
        mvc.perform(
            post("/admin/v1/test").cookie(login.cookie)
                .header("X-CSRF-TOKEN", csrf),
        ).andExpect(status().isOk)
    }

    @Test
    @DisplayName("DB에서 ADMIN을 회수하면 기존 세션의 다음 요청부터 차단한다")
    fun givenAdminSession_whenAdminRevoked_thenNextRequestForbidden() {
        val user: User = createUser(RoleType.ADMIN.role)
        val login = login(user)
        mvc.perform(get("/admin/v1/test").cookie(login.cookie)).andExpect(status().isOk)

        user.roles = RoleType.USER.role
        users.saveAndFlush(user)

        mvc.perform(get("/admin/v1/test").cookie(login.cookie)).andExpect(status().isForbidden)
        assertThat(me(login.cookie)["admin"].asBoolean()).isFalse()
    }

    @Test
    @DisplayName("ADMIN과 이름이 비슷한 다른 권한은 관리자 권한으로 인정하지 않는다")
    fun givenSimilarRole_whenRequestingAdmin_thenForbidden() {
        val login = login(createUser("ROLE_ADMINISTRATOR"))
        mvc.perform(get("/admin/v1/test").cookie(login.cookie)).andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("세션 사용자와 Google subject 연결이 달라지면 기존 쿠키로 접근할 수 없다")
    fun givenChangedGoogleIdentity_whenRequestingAdmin_thenForbidden() {
        val user: User = createUser(RoleType.ADMIN.role)
        val login = login(user)
        user.oid = "different-subject"
        users.saveAndFlush(user)

        mvc.perform(get("/admin/v1/test").cookie(login.cookie)).andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("DB에서 사용자가 삭제되면 세션이 남아 있어도 관리 API는 거부한다")
    fun givenDeletedUser_whenRequestingAdmin_thenForbidden() {
        val user: User = createUser(RoleType.ADMIN.role)
        val login = login(user)
        users.deleteById(user.id!!)

        mvc.perform(get("/admin/v1/test").cookie(login.cookie)).andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("ADMIN이어도 CSRF 토큰이 없거나 틀린 변경 요청은 실행되지 않는다")
    fun givenAdminSession_whenCsrfMissingOrInvalid_thenWriteNotExecuted() {
        val login = login(createUser(RoleType.ADMIN.role))

        mvc.perform(post("/admin/v1/test").cookie(login.cookie)).andExpect(status().isForbidden)
        mvc.perform(
            post("/admin/v1/test").cookie(login.cookie)
                .header("X-CSRF-TOKEN", "invalid-token"),
        ).andExpect(status().isForbidden)
        assertThat(routes.writes.get()).isZero()
    }

    @Test
    @DisplayName("me에서 받은 CSRF 토큰으로 변경 요청을 수행하고 새로 조회한 토큰도 사용할 수 있다")
    fun givenAdminSession_whenUsingCsrfFromMe_thenWriteAllowed() {
        val login = login(createUser(RoleType.ADMIN.role))
        repeat(2) {
            val csrf: String = csrf(login.cookie)
            mvc.perform(
                post("/admin/v1/test").cookie(login.cookie)
                    .header("X-CSRF-TOKEN", csrf),
            ).andExpect(status().isOk)
        }
        assertThat(routes.writes.get()).isEqualTo(2)
    }

    @Test
    @DisplayName("대기 사용자도 CSRF로 로그아웃할 수 있고 이후 기존 세션을 재사용할 수 없다")
    fun givenPendingSession_whenLoggingOutWithCsrf_thenSessionDeleted() {
        val login = login(createUser())
        val csrf: String = csrf(login.cookie)
        mvc.perform(post("/admin/v1/auth/logout").cookie(login.cookie)).andExpect(status().isForbidden)
        mvc.perform(
            post("/admin/v1/auth/logout").cookie(login.cookie)
                .header("X-CSRF-TOKEN", csrf),
        ).andExpect(status().isOk)

        assertThat(sessions.findById(login.id)).isNull()
        mvc.perform(get("/admin/v1/auth/me").cookie(login.cookie)).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("유휴 시간이 30분을 지난 JDBC 세션으로는 접근할 수 없다")
    fun givenExpiredSession_whenRequestingAdmin_thenUnauthorized() {
        val login = login(createUser(RoleType.ADMIN.role))
        val session = sessions.findById(login.id)!!
        session.lastAccessedTime = Instant.now().minus(Duration.ofMinutes(31))
        sessions.save(session)

        mvc.perform(get("/admin/v1/test").cookie(login.cookie)).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("Google 로그인 시작은 인증 요청을 JDBC 세션에 저장하고 Google로 이동한다")
    fun givenAnonymous_whenStartingGoogleLogin_thenStateAndSessionPersisted() {
        val response = mvc.perform(get("/oauth2/authorization/google"))
            .andExpect(status().is3xxRedirection).andReturn().response

        assertThat(response.redirectedUrl).startsWith("https://accounts.google.com/")
            .contains("state=").contains("nonce=").contains("response_type=code")
        assertThat(response.getCookie("ADMINSESSION")).isNotNull()
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ADMIN_SESSION", Int::class.java)).isEqualTo(1)
    }

    @Test
    @DisplayName("저장된 로그인 요청과 일치하지 않는 Google 콜백은 인증에 실패한다")
    fun givenUnknownState_whenReceivingGoogleCallback_thenUnauthorized() {
        mvc.perform(
            get("/login/oauth2/code/google").param("code", "unused-code").param("state", "unknown-state"),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("PostgreSQL에서 동시에 최초 로그인해도 Google 계정은 하나만 생성된다")
    fun givenConcurrentFirstLogins_whenCreatingSameGoogleSubject_thenOneAccount() {
        val database: String = jdbc.dataSource!!.connection.use { it.metaData.databaseProductName }
        assumeTrue(database == "PostgreSQL", "실제 Google partial unique 제약은 PostgreSQL에서 검증한다")
        val subject: String = UUID.randomUUID().toString()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = (1..8).map {
                executor.submit<Long> {
                    start.await()
                    accounts.findOrCreate(subject, "concurrent@example.com", "동시 로그인")
                }
            }
            start.countDown()
            val ids: List<Long> = futures.map { it.get(20, TimeUnit.SECONDS) }
            assertThat(ids.toSet()).hasSize(1)
            assertThat(users.count()).isEqualTo(1)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun createUser(roles: String = RoleType.USER.role): User = users.saveAndFlush(
        User(
            email = "admin-${UUID.randomUUID()}@example.com",
            name = "관리자",
            oid = UUID.randomUUID().toString(),
            roles = roles,
            providerType = ProviderType.valueOf("GOOGLE"),
            profileImageId = null,
        ),
    )

    private fun login(user: User): Login {
        val now: Instant = Instant.now()
        val idToken = OidcIdToken(
            "test-id-token",
            now,
            now.plusSeconds(3600),
            mapOf("sub" to user.oid!!, "email" to user.email!!, "email_verified" to true),
        )
        val delegate = DefaultOidcUser(listOf(OidcUserAuthority(idToken)), idToken)
        val principal = AdminPrincipal(user.id!!, delegate)
        val authentication = OAuth2AuthenticationToken(principal, principal.authorities, "google")
        val session = sessions.createSession()
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextImpl(authentication),
        )
        sessions.save(session)
        val encoded: String = Base64.getEncoder().encodeToString(session.id.toByteArray(Charsets.UTF_8))
        return Login(session.id, Cookie("ADMINSESSION", encoded))
    }

    private fun me(cookie: Cookie): JsonNode {
        val response = mvc.perform(get("/admin/v1/auth/me").cookie(cookie))
            .andExpect(status().isOk).andReturn().response
        assertThat(response.getHeader("X-CSRF-TOKEN")).isNotBlank()
        val body: JsonNode = objectMapper.readTree(response.contentAsString)
        assertThat(body["resultCode"].asText()).isEqualTo("D-0")
        return body["data"]
    }

    private fun csrf(cookie: Cookie): String = mvc.perform(get("/admin/v1/auth/me").cookie(cookie))
        .andExpect(status().isOk).andReturn().response.getHeader("X-CSRF-TOKEN")!!

    private data class Login(val id: String, val cookie: Cookie)
}

@TestComponent
@RestController
@RequestMapping("/admin/v1/test")
class AdminAuthTestController {
    val writes = AtomicInteger()

    @GetMapping
    fun read(): BaseResponse<String> = BaseResponse(data = "ok")

    @PostMapping
    fun write(): BaseResponse<Int> = BaseResponse(data = writes.incrementAndGet())
}
