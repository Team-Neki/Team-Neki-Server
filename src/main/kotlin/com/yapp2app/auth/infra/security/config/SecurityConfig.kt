package com.yapp2app.auth.infra.security.config

import com.nimbusds.jose.shaded.gson.JsonObject
import com.yapp2app.auth.infra.security.filter.AuthMdcFilter
import com.yapp2app.auth.infra.security.filter.JwtAuthenticationFilter
import com.yapp2app.common.api.dto.ResultCode
import jakarta.servlet.http.HttpServletResponse
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
    ): SecurityFilterChain = http
        .securityMatcher("/**")
        .csrf { it.disable() }
        .cors { it.configurationSource(corsConfigurationSource) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/auth/**", "/api/users/register").permitAll()
            it.anyRequest().authenticated()
        }
        // 토큰을 아예 입력하지 않았을 경우 아래 로직 수행
        .exceptionHandling {
            it.authenticationEntryPoint { _, response, _ ->
                val jsonObject = JsonObject()
                response.contentType = "application/json;charset=UTF-8"
                response.characterEncoding = "utf-8"
                response.status = HttpServletResponse.SC_FORBIDDEN

                jsonObject.addProperty("resultCode", ResultCode.INVALID_TOKEN_ERROR.code)
                jsonObject.addProperty("message", ResultCode.INVALID_TOKEN_ERROR.message)
                jsonObject.add("data", null)

                response.writer.print(jsonObject)
                response.writer.flush()
            }
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .addFilterAfter(AuthMdcFilter(), JwtAuthenticationFilter::class.java)
        .build()
}
