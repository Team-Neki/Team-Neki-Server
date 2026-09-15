package com.neki.domain.user.infra.security.config

import com.neki.domain.user.infra.security.filter.AuthMdcFilter
import com.neki.domain.user.infra.security.filter.JwtAuthenticationFilter
import com.neki.domain.user.infra.security.handler.CustomAccessDeniedHandler
import com.neki.domain.user.infra.security.handler.CustomAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
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
     * Actuator 엔드포인트 보안 설정 (Kubernetes Probe + Prometheus 메트릭)
     */
    @Bean
    @Order(0)
    fun actuatorSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http.securityMatcher("/actuator/health/**", "/actuator/prometheus", "/actuator/info", "/actuator/metrics/**")
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()

    /**
     * Swagger UI / API docs 는 local, staging 에서만 공개한다.
     * 운영에서는 이 체인을 등록하지 않아 Swagger 경로 요청이
     * apiSecurityFilterChain 의 authenticated() 에 걸리도록 한다.
     */
    @Bean
    @Order(1)
    @Profile("!prod & !production")
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
            it.requestMatchers("/api/auth/**", "/api/users/register", "/api/versions/**", "/api/terms").permitAll()
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
