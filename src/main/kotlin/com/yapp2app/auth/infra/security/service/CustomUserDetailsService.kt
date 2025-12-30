package com.yapp2app.auth.infra.security.service

import com.yapp2app.auth.infra.security.token.UserPrincipal
import com.yapp2app.user.application.port.UserRepositoryPort
import com.yapp2app.user.domain.enums.ProviderType
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * fileName       : CustomUserDetailsService
 * author         : darren
 * date           : 2025. 12. 30.
 * description    : 커스텀 UserDetailsService - oid와 providerType으로 사용자 인증
 */
@Service
class CustomUserDetailsService(private val userRepositoryPort: UserRepositoryPort) : UserDetailsService {

    /**
     * username 형식: "oid:providerType" (예: "123456:KAKAO")
     */
    override fun loadUserByUsername(username: String): UserDetails {
        val parts = username.split(":")

        if (parts.size != 2) {
            throw UsernameNotFoundException("Invalid username format. Expected: 'oid:providerType'")
        }

        val oid = parts[0].toLongOrNull() ?: throw UsernameNotFoundException("Invalid oid format")
        val providerType = ProviderType.valueOf(parts[1])

        val user = userRepositoryPort.findByOid(oid, providerType)
            ?: throw UsernameNotFoundException("User not found with oid: $oid, providerType: $providerType")

        return UserPrincipal(user)
    }
}
