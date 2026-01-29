package com.yapp2app.auth.infra.security.config

import com.yapp2app.auth.infra.security.filter.AuthMdcFilter
import com.yapp2app.auth.infra.security.filter.JwtAuthenticationFilter
import com.yapp2app.auth.infra.security.handler.CustomAccessDeniedHandler
import com.yapp2app.auth.infra.security.handler.CustomAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

/**
 * Swagger 및 API 문서 관련 보안 설정
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(private val corsConfigurationSource: CorsConfigurationSource) {

    /**
     * Actuator Health Check 엔드포인트 보안 설정 (Kubernetes Probe용)
     */
    @Bean
    @Order(0)
    fun actuatorSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http.securityMatcher("/actuator/health/**")
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()

    @Bean
    @Order(1)
    fun documentSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http.securityMatcher("/swagger-ui/**", "/v3/api-docs/**")
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource) }
            .logout { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()

    /**
     * 정적 파일 (이미지) 엔드포인트 보안 설정
     */
    @Bean
    @Order(2)
    fun staticFileSecurityFilterChain(http: HttpSecurity): SecurityFilterChain = http.securityMatcher("/file/**")
        .csrf { it.disable() }
        .cors { it.configurationSource(corsConfigurationSource) }
        .authorizeHttpRequests { it.anyRequest().permitAll() }
        .build()

    @Bean
    @Order(3)
    fun apiSecurityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        authenticationEntryPoint: CustomAuthenticationEntryPoint,
        accessDeniedHandler: CustomAccessDeniedHandler,
    ): SecurityFilterChain = http
        .securityMatcher("/**")
        .csrf { it.disable() }
        .cors { it.configurationSource(corsConfigurationSource) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/auth/**", "/api/users/register").permitAll()
            it.requestMatchers("/api/poses/admin/upload").hasRole("ADMIN")
            it.anyRequest().authenticated()
        }
        .exceptionHandling {
            it.authenticationEntryPoint(authenticationEntryPoint)
            it.accessDeniedHandler(accessDeniedHandler)
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .addFilterAfter(AuthMdcFilter(), JwtAuthenticationFilter::class.java)
        .build()
}
