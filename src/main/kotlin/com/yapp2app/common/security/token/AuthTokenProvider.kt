package com.yapp2app.common.security.token

import com.yapp2app.common.security.properties.AppProperties
import com.yapp2app.auth.domain.entity.User
import com.yapp2app.auth.domain.enums.ProviderType
import com.yapp2app.auth.domain.enums.RoleType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class AuthTokenProvider(private val appProperties: AppProperties) {

    companion object {
        private const val AUTHORITIES_KEY = "roles"
        private const val NAME_KEY = "name"
        private const val PROVIDER_TYPE_KEY = "provider_type"
    }

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(appProperties.auth.tokenSecret?.toByteArray() ?: byteArrayOf())
    }

    fun createToken(id: String): String = createToken(
        id = id,
        roles = listOf(RoleType.USER.role),
        name = null,
        providerType = null,
    )

    fun createToken(id: String, roles: Collection<String>, providerType: ProviderType): String = createToken(
        id = id,
        roles = roles,
        name = null,
        providerType = providerType,
    )

    fun createToken(id: String, name: String, roles: Collection<String>, providerType: ProviderType): String =
        createToken(
            id = id,
            roles = roles,
            name = name,
            providerType = providerType,
        )

    private fun createToken(id: String, roles: Collection<String>, name: String?, providerType: ProviderType?): String {
        val now = Instant.now()
        val expiryMillis = appProperties.auth.tokenExpiry ?: 0L

        return Jwts.builder()
            .subject(id)
            .claim(AUTHORITIES_KEY, roles)
            .apply {
                name?.let { claim(NAME_KEY, it) }
                providerType?.let { claim(PROVIDER_TYPE_KEY, it) }
            }
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expiryMillis)))
            .signWith(secretKey)
            .compact()
    }

    fun getAuthentication(token: String): Authentication {
        val claims = getTokenClaims(token)

        @Suppress("UNCHECKED_CAST")
        val roles = (claims[AUTHORITIES_KEY] as? List<*>)
            ?.filterIsInstance<String>()
            ?: emptyList()

        val name = claims[NAME_KEY] as? String ?: ""
        val providerTypeStr = claims[PROVIDER_TYPE_KEY] as? String
            ?: throw IllegalArgumentException("Provider type not found in token")

        val authorities = roles.map { SimpleGrantedAuthority(it) }

        val principal = UserPrincipal(
            User(
                email = claims.subject,
                name = name,
                roles = roles.joinToString(","),
                providerType = ProviderType.valueOf(providerTypeStr),
            ),
        )

        return UsernamePasswordAuthenticationToken(principal, token, authorities)
    }

    private fun getTokenClaims(token: String): Claims = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .payload
}
