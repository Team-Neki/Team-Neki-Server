package com.yapp2app.auth.infra.security.config

import com.yapp2app.auth.infra.security.properties.AppProperties
import com.yapp2app.auth.infra.security.token.AuthTokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfigurationSource

/**
 * Swagger 및 API 문서 관련 보안 설정
 */
@Configuration
@EnableWebSecurity
class DocumentSecurityConfig(
    private val authTokenProvider: AuthTokenProvider,
    private val appProperties: AppProperties,
    private val corsConfigurationSource: CorsConfigurationSource,
) {

    /**
     * token 발급 전 test config
     * token 발급 로직 후 삭제
     */
    @Deprecated("test config")
    @Bean
    @Order(-1) // 가장 먼저 적용되도록 우선순위 설정
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain = http.securityMatcher("/api/media/test/**")
        .csrf { it.disable() }
        .cors { it.configurationSource(corsConfigurationSource) }
        .authorizeHttpRequests { it.anyRequest().permitAll() }
        .build()

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
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .securityMatcher("/api/**")
        .csrf { it.disable() }
        .cors { it.configurationSource(corsConfigurationSource) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/auth/**").permitAll()
            it.anyRequest().authenticated()
        }
        .build()
}
