package com.neki.api.common.api.document

import com.neki.core.code.ResultCode
import com.neki.core.properties.AppProperties
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
            .info(Info().title("Neki API Document").version(appProperties.version))
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
                addErrorResponseIfMissing(
                    HttpStatus.BAD_REQUEST,
                    ResultCode.INVALID_PARAMETER,
                    "message 값을 모달로 띄워주세요.",
                )
                if (isSecured) {
                    addErrorResponseIfMissing(HttpStatus.UNAUTHORIZED, ResultCode.EXPIRED_TOKEN_ERROR)
                    addErrorResponseIfMissing(HttpStatus.FORBIDDEN, ResultCode.INVALID_TOKEN_ERROR)
                    addErrorResponseIfMissing(HttpStatus.FORBIDDEN, ResultCode.MISSING_TOKEN_ERROR)
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

private fun Operation.addErrorResponseIfMissing(
    status: HttpStatus,
    resultCode: ResultCode,
    clientGuide: String? = null,
) {
    val code = status.value().toString()
    val existing = responses[code]

    if (existing == null) {
        responses.addApiResponse(code, errorResponse(resultCode, clientGuide))
    } else if (existing.content == null || existing.content.isEmpty()) {
        existing.content = errorResponse(resultCode, clientGuide).content
    }
}

private fun errorResponse(resultCode: ResultCode, clientGuide: String? = null): ApiResponse = ApiResponse().apply {
    val messageWithGuide = if (clientGuide != null) {
        "${resultCode.message} ($clientGuide)"
    } else {
        resultCode.message
    }
    description = clientGuide ?: resultCode.message

    val errorSchema = Schema<Any>()
        .type("object")
        .addProperty("resultCode", Schema<String>().type("string").example(resultCode.code))
        .addProperty("message", Schema<String>().type("string").example(messageWithGuide))
        .addProperty("data", Schema<Any>().nullable(true).example(null))

    val exampleBody = mapOf(
        "resultCode" to resultCode.code,
        "message" to messageWithGuide,
        "data" to null,
    )

    content = Content().addMediaType(
        "application/json",
        MediaType().schema(errorSchema).example(exampleBody),
    )
}
