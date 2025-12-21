package com.yapp2app.common.api.config

import com.yapp2app.auth.infra.security.properties.AppProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * fileName       : WebConfig
 * author         : koo
 * date           : 2025. 12. 22.
 * description    : Web/API 계층 설정 (CORS, Interceptor 등)
 *                  Presentation Layer 관심사
 */
@Configuration
class WebConfig(private val appProperties: AppProperties) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // 허용할 Origin 설정
        val allowedOrigins = appProperties.cors.allowedOrigins.ifEmpty {
            listOf("http://localhost:3000", "http://127.0.0.1:3000", "file://")
        }
        configuration.allowedOrigins = allowedOrigins

        // 허용할 HTTP Method
        configuration.allowedMethods = listOf(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS",
        )

        // 허용할 Header
        configuration.allowedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
        )

        // 인증 정보 포함 허용
        configuration.allowCredentials = true

        // Preflight 요청 캐시 시간 (초)
        configuration.maxAge = 3600L

        // 노출할 Header
        configuration.exposedHeaders = listOf(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
        )

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }
}
