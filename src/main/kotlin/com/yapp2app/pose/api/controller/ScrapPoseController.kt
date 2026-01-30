package com.yapp2app.pose.api.controller

import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.api.converter.ScrapPoseCommandConverter
import com.yapp2app.pose.api.converter.ScrapPoseResultConverter
import com.yapp2app.pose.api.dto.GetPosesResponse
import com.yapp2app.pose.api.dto.UpdatePoseScarpRequest
import com.yapp2app.pose.application.usecase.GetScrapPosesUseCase
import com.yapp2app.pose.application.usecase.UpdatePoseScrapUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : ScrapPoseController
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@RequiresSecurity
@Tag(name = "scrap pose", description = "포즈 스크랩")
@RestController
@RequestMapping("/api/poses")
class ScrapPoseController(
    private val updatePoseScrapUseCase: UpdatePoseScrapUseCase,
    private val getScrapPosesUseCase: GetScrapPosesUseCase,

    private val commandConverter: ScrapPoseCommandConverter,
    private val resultConverter: ScrapPoseResultConverter,
) {

    @Operation(
        summary = "포즈 스크랩",
        description = "포즈를 스크랩 합니다. 멱등성 보장을 위해 body에 변경하고자하는 scrap 상태를 입력하면 됩니다.",
    )
    @PatchMapping("/{poseId}/scrap")
    fun scrapPose(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable poseId: Long,
        @Valid @RequestBody request: UpdatePoseScarpRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toUpdatePoseScrapCommand(userId = userId, poseId = poseId, request = request)

        updatePoseScrapUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "포즈 스크랩 목록 API",
        description = "포즈 스크랩 목록을 조회합니다. Offset 기반 페이징을 지원합니다.",
    )
    @GetMapping("/scrap")
    fun scrapPoseList(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
    ): BaseResponse<GetPosesResponse> {
        val command = commandConverter.toGetPoseScrapCommand(
            userId = userId,
            page = page,
            size = size,
            sortOrder = sortOrder,
        )

        val result = getScrapPosesUseCase.execute(command)

        val response = resultConverter.toGetPosesResponse(result)

        return BaseResponse(data = response)
    }
}
