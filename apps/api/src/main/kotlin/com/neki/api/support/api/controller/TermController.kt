package com.neki.api.support.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.support.api.dto.TermConverter
import com.neki.api.support.api.dto.TermRequest
import com.neki.api.support.api.dto.TermResponse
import com.neki.api.support.application.CreateTermAgreementsUseCase
import com.neki.api.support.application.GetTermsUseCase
import com.neki.api.support.application.dto.TermResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.support.dto.TermCommand
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
    private val requestConverter: TermConverter.RequestConverter,
    private val responseConverter: TermConverter.ResponseConverter,
) {

    @Operation(
        summary = "약관 목록 조회",
        description = "현재 활성화된 약관 목록을 조회합니다.",
    )
    @GetMapping
    fun getTerms(): BaseResponse<TermResponse.GetTerms> {
        val result: TermResult.GetTerms = getTermsUseCase.execute()
        val response: TermResponse.GetTerms = responseConverter.toGetTermsResponse(result)
        return BaseResponse(data = response)
    }

    @RequiresSecurity
    @Operation(
        summary = "약관 동의",
        description = "약관 동의를 처리합니다. 최초 가입 시 필수 약관에 모두 동의해야 하며, 선택 약관은 동의/미동의를 변경할 수 있습니다.",
    )
    @PostMapping("/agreements")
    fun createTermAgreements(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: TermRequest.CreateTermAgreements,
    ): BaseResponse<Any> {
        val command: TermCommand.CreateTermAgreements = requestConverter.toCreateTermAgreementsCommand(userId, request)
        createTermAgreementsUseCase.execute(command)
        return BaseResponse()
    }
}
