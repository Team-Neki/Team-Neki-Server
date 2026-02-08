package com.yapp2app.term.api.controller

import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.term.api.converter.TermCommandConverter
import com.yapp2app.term.api.converter.TermResultConverter
import com.yapp2app.term.api.dto.CreateTermAgreementsRequest
import com.yapp2app.term.api.dto.GetTermsResponse
import com.yapp2app.term.application.usecase.CreateTermAgreementsUseCase
import com.yapp2app.term.application.usecase.GetTermsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Term", description = "약관 API")
@RestController
@RequestMapping("/api/terms")
class TermController(
    private val getTermsUseCase: GetTermsUseCase,
    private val createTermAgreementsUseCase: CreateTermAgreementsUseCase,
    private val commandConverter: TermCommandConverter,
    private val resultConverter: TermResultConverter,
) {

    @Operation(
        summary = "약관 목록 조회",
        description = "현재 활성화된 약관 목록을 조회합니다.",
    )
    @GetMapping
    fun getTerms(): BaseResponse<GetTermsResponse> {
        val result = getTermsUseCase.execute()
        val response = resultConverter.toGetTermsResponse(result)
        return BaseResponse(data = response)
    }

    @RequiresSecurity
    @Operation(
        summary = "약관 동의",
        description = "약관에 동의합니다. 필수 약관에 모두 동의해야 합니다.",
    )
    @PostMapping("/agreements")
    fun createTermAgreements(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: CreateTermAgreementsRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toCreateTermAgreementsCommand(userId, request)
        createTermAgreementsUseCase.execute(command)
        return BaseResponse()
    }
}
