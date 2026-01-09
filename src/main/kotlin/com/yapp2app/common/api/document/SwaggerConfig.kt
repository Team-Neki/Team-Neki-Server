package com.yapp2app.common.api.document

import com.yapp2app.auth.infra.security.properties.AppProperties
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus

@Configuration
class SwaggerConfig(private val appProperties: AppProperties) {

    companion object {
        private const val SECURITY_SCHEME = "JWT"
    }

    @Bean
    fun openAPI(): OpenAPI {
        val components: Components = Components()
            .addSecuritySchemes(
                SECURITY_SCHEME,
                SecurityScheme()
                    .name(SECURITY_SCHEME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat(SECURITY_SCHEME),
            )

        return OpenAPI()
            // TODO : 도메인 추가 후 수정
            .addServersItem(
                Server()
                    .url(appProperties.server.url)
                    .description("http server (no ssl)"),
            )
            .components(components)
            .info(Info().title("Yapp App Team2 API Document").version(appProperties.version))
    }

    @Bean
    fun generalApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("general")
        .pathsToMatch("/api/**")
        .addOperationCustomizer(customize())
        .build()

    @Bean
    fun customize() = OperationCustomizer { operation, handlerMethod ->

        val isSecured = handlerMethod.getMethodAnnotation(RequiresSecurity::class.java) != null ||
            handlerMethod.beanType.getAnnotation(RequiresSecurity::class.java) != null

        operation
            .withSecurity(isSecured, SECURITY_SCHEME)
            .ensureResponses()
            .apply {
                addErrorResponseIfMissing(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error")
                if (isSecured) {
                    addErrorResponseIfMissing(HttpStatus.UNAUTHORIZED, "Unauthorized")
                    addErrorResponseIfMissing(HttpStatus.FORBIDDEN, "Forbidden")
                }
            }
    }
}

private fun Operation.withSecurity(isSecured: Boolean, schemeName: String): Operation = apply {
    security(
        if (isSecured) {
            listOf(SecurityRequirement().addList(schemeName))
        } else {
            emptyList()
        },
    )
}

private fun Operation.ensureResponses(): Operation = apply { responses = responses ?: ApiResponses() }

private fun Operation.addErrorResponseIfMissing(status: HttpStatus, message: String) {
    val code = status.value().toString()
    val existing = responses[code]

    if (existing == null) {
        responses.addApiResponse(code, errorResponse(status, message))
    } else if (existing.content == null || existing.content.isEmpty()) {
        existing.content = errorResponse(status, message).content
    }
}

/**
 * TODO : 공통 응답 스키마로 변경 필요
 */
private fun errorResponse(status: HttpStatus, message: String): ApiResponse = ApiResponse().apply {
    description = message

    val errorSchema = Schema<Any>()
        .type("object")
        .addProperty("status", Schema<Int>().type("integer").example(status.value()))
        .addProperty("message", Schema<String>().type("string").example(message))
        .addProperty(
            "timestamp",
            Schema<String>().type("string")
                .format("date-time")
                .example("2025-12-05T00:00:00"),
        )

    val exampleBody = mapOf(
        "status" to status.value(),
        "message" to message,
        "timestamp" to "2025-12-05T00:00:00",
    )

    content = Content().addMediaType(
        "application/json",
        MediaType().schema(errorSchema).example(exampleBody),
    )
}
