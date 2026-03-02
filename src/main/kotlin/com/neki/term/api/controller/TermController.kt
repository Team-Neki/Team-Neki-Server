package com.neki.term.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.term.api.converter.TermCommandConverter
import com.neki.term.api.converter.TermResultConverter
import com.neki.term.api.dto.CreateTermAgreementsRequest
import com.neki.term.api.dto.GetTermsResponse
import com.neki.term.application.command.CreateTermAgreementsCommand
import com.neki.term.application.result.GetTermsResult
import com.neki.term.application.usecase.CreateTermAgreementsUseCase
import com.neki.term.application.usecase.GetTermsUseCase
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
        val result: GetTermsResult = getTermsUseCase.execute()
        val response: GetTermsResponse = resultConverter.toGetTermsResponse(result)
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
        val command: CreateTermAgreementsCommand = commandConverter.toCreateTermAgreementsCommand(userId, request)
        createTermAgreementsUseCase.execute(command)
        return BaseResponse()
    }
}
