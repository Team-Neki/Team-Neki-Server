package com.yapp2app.auth.infra.security.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * fileName       : PasswordConfig
 * author         : koo
 * date           : 2025. 12. 28. 오후 7:49
 * description    :
 */
@Deprecated("password config for local password encrypt")
@Configuration
class PasswordConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        val encoders = mapOf(
            "bcrypt" to BCryptPasswordEncoder(),
            "noop" to NoOpPasswordEncoder.getInstance(),
        )

        return DelegatingPasswordEncoder("noop", encoders)
    }
}
