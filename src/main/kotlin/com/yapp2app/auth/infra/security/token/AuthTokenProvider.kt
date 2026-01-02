package com.yapp2app.auth.infra.security.token

import com.yapp2app.auth.infra.security.properties.AppProperties
import com.yapp2app.user.domain.enums.ProviderType
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

/**
 * fileName       : AuthTokenProvider
 * author         : darren
 * date           : 2025. 12. 18. 18:45
 * description    : JWT 토큰 제공 클래스
 */
@Component
class AuthTokenProvider(private val appProperties: AppProperties) {

    companion object {
        private const val AUTHORITIES_KEY = "roles"
        private const val NAME_KEY = "name"
        private const val PROVIDER_TYPE_KEY = "provider_type"
    }

    private val accessTokenSecretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(appProperties.auth.accessTokenSecret?.toByteArray() ?: byteArrayOf())
    }

    private val refreshTokenSecretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(appProperties.auth.refreshTokenSecret?.toByteArray() ?: byteArrayOf())
    }

    fun createAccessToken(id: String, name: String?, roles: Collection<String>, providerType: ProviderType): String {
        val now = Instant.now()
        val expiryMillis = appProperties.auth.accessTokenExpiry ?: 0L

        return Jwts.builder()
            .subject(id)
            .claim(AUTHORITIES_KEY, roles)
            .apply {
                name?.let { claim(NAME_KEY, it) }
                providerType?.let { claim(PROVIDER_TYPE_KEY, it) }
            }
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expiryMillis)))
            .signWith(accessTokenSecretKey)
            .compact()
    }

    fun createRefreshToken(id: String, name: String?, roles: Collection<String>, providerType: ProviderType): String {
        val now = Instant.now()
        val expiryMillis = appProperties.auth.refreshTokenExpiry ?: 0L

        return Jwts.builder()
            .subject(id)
            .claim(AUTHORITIES_KEY, roles)
            .apply {
                name?.let { claim(NAME_KEY, it) }
                providerType?.let { claim(PROVIDER_TYPE_KEY, it) }
            }
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expiryMillis)))
            .signWith(refreshTokenSecretKey)
            .compact()
    }

    fun getAuthentication(token: String): Authentication {
        val claims = getAccessTokenClaims(token)

        @Suppress("UNCHECKED_CAST")
        val roles = (claims[AUTHORITIES_KEY] as? List<*>)
            ?.filterIsInstance<String>()
            ?: emptyList()

        val name = claims[NAME_KEY] as? String ?: ""
        val providerTypeStr = claims[PROVIDER_TYPE_KEY] as? String
            ?: throw IllegalArgumentException("Provider type not found in token")

        val authorities = roles.map { SimpleGrantedAuthority(it) }

        val principal = UserPrincipal(
            id = claims.subject.toLong(), // JWT subject(String)를 Long으로 변환
            name = name,
            providerType = ProviderType.valueOf(providerTypeStr),
            email = "", // 토큰에서는 email 정보 없음 (필요시 claim 추가)
            roles = roles.toSet(),
            password = "NO_PASS",
        )

        return UsernamePasswordAuthenticationToken(principal, token, authorities)
    }

    private fun getAccessTokenClaims(token: String): Claims = Jwts.parser()
        .verifyWith(accessTokenSecretKey)
        .build()
        .parseSignedClaims(token)
        .payload

    private fun getRefreshTokenClaims(token: String): Claims = Jwts.parser()
        .verifyWith(refreshTokenSecretKey)
        .build()
        .parseSignedClaims(token)
        .payload

    fun validateRefreshToken(token: String): Boolean {
        return try {
            getRefreshTokenClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAuthenticationFromRefreshToken(token: String): Authentication {
        val claims = getRefreshTokenClaims(token)

        @Suppress("UNCHECKED_CAST")
        val roles = (claims[AUTHORITIES_KEY] as? List<*>)
            ?.filterIsInstance<String>()
            ?: emptyList()

        val name = claims[NAME_KEY] as? String ?: ""
        val providerTypeStr = claims[PROVIDER_TYPE_KEY] as? String
            ?: throw IllegalArgumentException("Provider type not found in token")

        val authorities = roles.map { SimpleGrantedAuthority(it) }

        val principal = UserPrincipal(
            id = claims.subject.toLong(),
            name = name,
            providerType = ProviderType.valueOf(providerTypeStr),
            email = "",
            roles = roles.toSet(),
            password = "NO_PASS",
        )

        return UsernamePasswordAuthenticationToken(principal, token, authorities)
    }
}
