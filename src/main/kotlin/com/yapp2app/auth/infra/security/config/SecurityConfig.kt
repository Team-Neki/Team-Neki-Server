package com.yapp2app.auth.infra.security.config

import com.yapp2app.auth.infra.security.filter.JwtAuthenticationFilter
import com.yapp2app.auth.infra.security.service.CustomUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

/**
 * Swagger 및 API 문서 관련 보안 설정
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(private val corsConfigurationSource: CorsConfigurationSource) {

    @Bean
    @Order(0)
    fun documentSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http.securityMatcher("/swagger-ui/**", "/v3/api-docs/**")
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource) }
            .logout { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()

    @Bean
    @Order(1)
    fun apiSecurityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
    ): SecurityFilterChain = http
        .securityMatcher("/**")
        .csrf { it.disable() }
        .cors { it.configurationSource(corsConfigurationSource) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/auth/**", "/api/users/register").permitAll()
            it.anyRequest().authenticated()
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        // OAuth 전용이므로 NoOpPasswordEncoder 사용 (평문 비교)
        @Suppress("DEPRECATION")
        return NoOpPasswordEncoder.getInstance()
    }

    /**
     * AuthenticationManager 설정
     */
    @Bean
    fun authenticationManager(
        userDetailsService: CustomUserDetailsService,
        passwordEncoder: PasswordEncoder,
    ): AuthenticationManager {
        val authProvider = DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService)
            setPasswordEncoder(passwordEncoder)
        }
        return ProviderManager(authProvider)
    }
}
