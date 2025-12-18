package com.yapp2app.auth.infra.security.config

import com.yapp2app.auth.infra.security.properties.AppProperties
import com.yapp2app.auth.infra.security.token.AuthTokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Swagger 및 API 문서 관련 보안 설정
 */
@Configuration
@EnableWebSecurity
class DocumentSecurityConfig(
    private val authTokenProvider: AuthTokenProvider,
    private val appProperties: AppProperties,
) {

    @Bean
    @Order(0)
    fun documentSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http.securityMatcher("/swagger-ui/**", "/v3/api-docs/**")
            .csrf { it.disable() }
            .cors { it.disable() } // TODO : cors 설정 추가
            .logout { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()

    @Bean
    @Order(1)
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .securityMatcher("/api/**")
        .csrf { it.disable() }
        .cors { /* CORS 설정 */ }
        .authorizeHttpRequests {
            it.requestMatchers("/api/auth/**").permitAll()
            it.anyRequest().authenticated()
        }
        .build()
}
