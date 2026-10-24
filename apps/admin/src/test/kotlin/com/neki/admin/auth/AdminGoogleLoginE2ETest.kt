package com.neki.admin.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import com.neki.domain.user.infra.security.admin.AdminIdentityProvider
import com.neki.domain.user.infra.security.admin.AdminSocialProfile
import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import com.neki.domain.user.repository.UserRepository
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestComponent
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AlternateAdminIdentityProvider::class)
@DisplayName("실제 OIDC code 교환과 서명 토큰 검증을 통한 Google 로그인")
class AdminGoogleLoginE2ETest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var users: JpaUserRepository

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM ADMIN_SESSION_ATTRIBUTES")
        jdbc.update("DELETE FROM ADMIN_SESSION")
        users.deleteAllInBatch()
        provider.reset()
    }

    @Test
    @DisplayName("Google code를 교환하고 서명을 검증한 뒤 ADMIN 없는 사용자와 JDBC 세션을 만든다")
    fun givenVerifiedGoogleAccount_whenLoginCompletes_thenPendingUserAndSessionCreated() {
        val cookie: Cookie = completeLogin()
        val me: JsonNode = me(cookie)
        val user: User = users.findById(me["userId"].asLong()).orElseThrow()

        assertThat(user.providerType).isEqualTo(ProviderType.ADMIN_SSO)
        assertThat(user.oid).isEqualTo("google:${provider.subject}")
        assertThat(user.email).isEqualTo(provider.email)
        assertThat(user.name!!.codePointCount(0, user.name!!.length)).isLessThanOrEqualTo(10)
        assertThat(user.roles).isEqualTo(RoleType.USER.role)
        assertThat(me["admin"].asBoolean()).isFalse()
        assertThat(me.has("csrfToken")).isFalse()
        assertThat(provider.tokenRequests).isEqualTo(1)
        assertThat(userRepository.countByOidIsNotNull()).isZero()
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ADMIN_SESSION", Int::class.java)).isEqualTo(1)
    }

    @Test
    @DisplayName("기존 Google 계정의 ADMIN 권한을 보존하고 로그인 반복으로 계정을 중복 생성하지 않는다")
    fun givenExistingAdmin_whenGoogleLoginRepeated_thenSameUserAndRolesPreserved() {
        val user: User = createUser(ProviderType.ADMIN_SSO, "google:${provider.subject}", "ROLE_USER,ROLE_ADMIN")

        val first: JsonNode = me(completeLogin())
        val second: JsonNode = me(completeLogin())

        assertThat(first["userId"].asLong()).isEqualTo(user.id)
        assertThat(second["userId"].asLong()).isEqualTo(user.id)
        assertThat(second["admin"].asBoolean()).isTrue()
        assertThat(users.count()).isEqualTo(1)
        assertThat(users.findById(user.id!!).orElseThrow().roles).isEqualTo("ROLE_USER,ROLE_ADMIN")
    }

    @Test
    @DisplayName("이메일이 같은 카카오 계정의 ADMIN 권한을 Google 계정에 연결하지 않는다")
    fun givenSameEmailOnOtherProvider_whenGoogleLoginCompletes_thenSeparatePendingAccountCreated() {
        val existing: User = createUser(ProviderType.KAKAO, "kakao-account", RoleType.ADMIN.role)
        val me: JsonNode = me(completeLogin())

        assertThat(me["userId"].asLong()).isNotEqualTo(existing.id)
        assertThat(me["admin"].asBoolean()).isFalse()
        assertThat(users.count()).isEqualTo(2)
        assertThat(userRepository.countByOidIsNotNull()).isEqualTo(1)
    }

    @Test
    @DisplayName("같은 subject라도 다른 OAuth2 제공자는 별도 관리 계정으로 등록된다")
    fun givenSameSubjectFromTwoProviders_whenLoggingIn_thenSeparateAccountsCreated() {
        val google: JsonNode = me(completeLogin())
        val alternate: JsonNode = me(completeLogin(registrationId = "alternate"))

        assertThat(google["userId"].asLong()).isNotEqualTo(alternate["userId"].asLong())
        assertThat(users.findById(alternate["userId"].asLong()).orElseThrow().oid)
            .isEqualTo("alternate:${provider.subject}")
        assertThat(users.count()).isEqualTo(2)
    }

    @Test
    @DisplayName("서명은 유효해도 로그인 요청의 nonce와 다른 ID 토큰은 거부한다")
    fun givenWrongNonce_whenGoogleCallbackReceived_thenAuthenticationRejected() {
        provider.nonceOverride = "not-the-request-nonce"
        completeLogin(expectSuccess = false)
        assertThat(users.count()).isZero()
    }

    @Test
    @DisplayName("이메일이 검증되지 않은 Google 계정은 사용자 등록 전에 거부한다")
    fun givenUnverifiedEmail_whenGoogleCallbackReceived_thenAuthenticationRejected() {
        provider.emailVerified = false
        completeLogin(expectSuccess = false)
        assertThat(users.count()).isZero()
    }

    private fun completeLogin(expectSuccess: Boolean = true, registrationId: String = "google"): Cookie {
        val start = mvc.perform(get("/oauth2/authorization/$registrationId"))
            .andExpect(status().is3xxRedirection).andReturn().response
        val parameters: Map<String, String> = decodeQuery(URI(start.redirectedUrl!!).rawQuery)
        provider.nonce = parameters["nonce"].orEmpty()
        val initialCookie: Cookie = start.getCookie("ADMINSESSION")!!
        val callback = mvc.perform(
            get("/login/oauth2/code/$registrationId").cookie(initialCookie)
                .param("code", "accepted-code").param("state", parameters.getValue("state")),
        )
        if (!expectSuccess) {
            callback.andExpect(status().isUnauthorized)
            return initialCookie
        }
        val response = callback.andExpect(status().is3xxRedirection).andReturn().response
        assertThat(response.redirectedUrl).endsWith("/admin/v1/auth/me")
        val authenticatedCookie: Cookie = response.getCookie("ADMINSESSION")!!
        assertThat(authenticatedCookie.value).isNotEqualTo(initialCookie.value)
        return authenticatedCookie
    }

    private fun me(cookie: Cookie): JsonNode {
        val response = mvc.perform(get("/admin/v1/auth/me").cookie(cookie))
            .andExpect(status().isOk).andReturn().response
        assertThat(response.getHeader("X-CSRF-TOKEN")).isNotBlank()
        return objectMapper.readTree(response.contentAsString)["data"]
    }

    private fun createUser(providerType: ProviderType, subject: String, roles: String): User = users.saveAndFlush(
        User(
            email = provider.email,
            name = "관리자",
            oid = subject,
            roles = roles,
            providerType = providerType,
            profileImageId = null,
        ),
    )

    companion object {
        private val provider = LocalGoogleProvider()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                System.getenv("SPRING_DATASOURCE_URL")
                    ?: "jdbc:h2:mem:admin-google-login-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
            }
            listOf("google", "alternate").forEach { registrationId ->
                val prefix = "spring.security.oauth2.client.provider.$registrationId"
                registry.add("$prefix.issuer-uri") { provider.issuer }
                registry.add("$prefix.authorization-uri") { "${provider.issuer}/authorize" }
                registry.add("$prefix.token-uri") { "${provider.issuer}/token" }
                registry.add("$prefix.jwk-set-uri") { "${provider.issuer}/jwks" }
                registry.add("$prefix.user-info-uri") { "${provider.issuer}/userinfo" }
            }
        }

        @JvmStatic
        @AfterAll
        fun stopProvider() {
            provider.stop()
        }

        private fun decodeQuery(query: String): Map<String, String> = query.split('&').associate { pair ->
            val parts: List<String> = pair.split('=', limit = 2)
            URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to
                URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8)
        }
    }

    private class LocalGoogleProvider {
        private val mapper = ObjectMapper()
        private val key = RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate()
        private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val issuer: String = "http://127.0.0.1:${server.address.port}"
        val subject: String = "local-google-subject"
        val email: String = "verified-admin@example.com"

        @Volatile
        var nonce: String = ""

        @Volatile
        var nonceOverride: String? = null

        @Volatile
        var emailVerified: Boolean = true

        @Volatile
        var tokenRequests: Int = 0

        init {
            server.createContext("/.well-known/openid-configuration") { exchange ->
                exchange.json(
                    mapOf(
                        "issuer" to issuer,
                        "authorization_endpoint" to "$issuer/authorize",
                        "token_endpoint" to "$issuer/token",
                        "jwks_uri" to "$issuer/jwks",
                        "userinfo_endpoint" to "$issuer/userinfo",
                        "response_types_supported" to listOf("code"),
                        "subject_types_supported" to listOf("public"),
                        "id_token_signing_alg_values_supported" to listOf("RS256"),
                    ),
                )
            }
            server.createContext("/jwks") { exchange ->
                exchange.json(mapOf("keys" to listOf(key.toPublicJWK().toJSONObject())))
            }
            server.createContext("/token") { exchange ->
                val form: String = exchange.requestBody.readAllBytes().toString(Charsets.UTF_8)
                val parameters: Map<String, String> = decodeQuery(form)
                check(parameters["code"] == "accepted-code")
                check(parameters["grant_type"] == "authorization_code")
                tokenRequests++
                val now: Instant = Instant.now()
                val claims: JWTClaimsSet = JWTClaimsSet.Builder()
                    .issuer(issuer).subject(subject).audience("admin-test-client")
                    .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("nonce", nonceOverride ?: nonce).claim("email", email)
                    .claim("email_verified", emailVerified).claim("name", "Google 관리자 이름이 긴 경우")
                    .build()
                val header: JWSHeader = JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(key.keyID).type(JOSEObjectType.JWT).build()
                val jwt = SignedJWT(header, claims)
                jwt.sign(RSASSASigner(key.toPrivateKey()))
                exchange.json(
                    mapOf(
                        "access_token" to "local-access-token",
                        "token_type" to "Bearer",
                        "expires_in" to 3600,
                        "scope" to "openid email profile",
                        "id_token" to jwt.serialize(),
                    ),
                )
            }
            server.createContext("/userinfo") { exchange ->
                exchange.json(
                    mapOf(
                        "sub" to subject,
                        "email" to email,
                        "email_verified" to emailVerified,
                        "name" to "Google 관리자 이름이 긴 경우",
                    ),
                )
            }
            server.start()
        }

        fun reset() {
            nonce = ""
            nonceOverride = null
            emailVerified = true
            tokenRequests = 0
        }

        fun stop() = server.stop(0)

        private fun HttpExchange.json(value: Any) {
            val bytes: ByteArray = mapper.writeValueAsBytes(value)
            responseHeaders.set("Content-Type", "application/json")
            sendResponseHeaders(200, bytes.size.toLong())
            responseBody.use { it.write(bytes) }
            close()
        }
    }
}

@TestComponent
class AlternateAdminIdentityProvider : AdminIdentityProvider {
    override val registrationId: String = "alternate"

    override fun extract(user: OAuth2User): AdminSocialProfile = AdminSocialProfile(
        subject = user.attributes.getValue("sub") as String,
        email = user.attributes.getValue("email") as String,
        name = user.attributes["name"] as? String,
    )
}
