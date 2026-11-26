package com.neki.api.share.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.share.api.dto.ShareConverter
import com.neki.api.share.api.dto.ShareLinkRequest
import com.neki.api.share.api.dto.ShareLinkResponse
import com.neki.api.share.application.IssueShareLinkUseCase
import com.neki.api.share.application.dto.ShareResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.share.dto.ShareCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : ShareLinkController
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 사진 공유 링크 발급 API
 */
@RequiresSecurity
@Tag(name = "ShareLinkController", description = "사진 공유 링크 API")
@RestController
@RequestMapping("/api/shares")
class ShareLinkController(
    private val issueShareLinkUseCase: IssueShareLinkUseCase,
    private val requestConverter: ShareConverter.RequestConverter,
    private val responseConverter: ShareConverter.ResponseConverter,
) {

    @Operation(
        summary = "사진 공유 링크 발급 API",
        description = """내 사진의 공유 링크를 발급합니다. 링크는 기본 7일 뒤 만료됩니다(설정 app.share.expiration).
            같은 사진에 아직 만료되지 않은 링크가 있으면 새로 만들지 않고 그 링크를 돌려줍니다.
            url 은 공유 웹페이지 주소({app.share.web-url}/s/{token}[?{app.share.web-url-query}], 카톡 전송용)입니다.
            staging 은 ?env=dev 가 붙어 웹페이지가 개발 백엔드를 호출합니다.
            이미지 바이너리는 로그인 없이 GET /share/{token} 으로 받습니다(웹페이지가 호출).
            내 사진이 아니거나 없는(삭제된) 사진이면 400 D-04 를 반환합니다.
            토큰 없이 호출하면 403 D-996 을 반환합니다.""",
    )
    @PostMapping
    fun issueShareLink(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: ShareLinkRequest.IssueShareLink,
    ): BaseResponse<ShareLinkResponse.IssueShareLink> {
        val command: ShareCommand.IssueShareLink = requestConverter.toIssueShareLinkCommand(userId, request)

        val result: ShareResult.IssueShareLink = issueShareLinkUseCase.execute(command)

        val response: ShareLinkResponse.IssueShareLink = responseConverter.toIssueShareLinkResponse(result)

        return BaseResponse(data = response)
    }
}
