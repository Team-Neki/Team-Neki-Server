package com.neki.admin.common.api.document

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// 같은 origin 에서 로그인하면 브라우저가 ADMINSESSION 쿠키를 Swagger UI 요청에 자동으로 붙인다.
// CSRF 토큰도 쓰지 않으므로 security scheme 은 두지 않는다.
@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(Info().title("Neki Admin API Document").version("v1"))

    @Bean
    fun adminApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("admin")
        .pathsToMatch("/api/**")
        .build()
}
