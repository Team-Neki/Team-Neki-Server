package com.yapp2app.api

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Test", description = "테스트용 API")
@RestController
@RequestMapping("/api/test")
class TestController {

    @ApiResponses(
        ApiResponse(responseCode = "200", description = "테스트 엔드포인트가 정상적으로 작동합니다."),
    )
    @GetMapping
    fun testEndpoint(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf("message" to "Test endpoint is working!"))
}
