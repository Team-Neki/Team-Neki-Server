package com.neki.pose.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.common.domain.vo.SortOrder
import com.neki.pose.api.dto.GetPosesResponse
import com.neki.pose.api.dto.ScrapPoseConverter
import com.neki.pose.api.dto.UpdatePoseScarpRequest
import com.neki.pose.application.dto.PoseCommand
import com.neki.pose.application.dto.PoseQuery
import com.neki.pose.application.dto.PoseResult
import com.neki.pose.application.usecase.GetScrapPosesUseCase
import com.neki.pose.application.usecase.UpdatePoseScrapUseCase
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

    private val requestConverter: ScrapPoseConverter.RequestConverter,
    private val responseConverter: ScrapPoseConverter.ResponseConverter,
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
        val command: PoseCommand.UpdatePoseScrap = requestConverter.toUpdatePoseScrapCommand(
            userId = userId,
            poseId = poseId,
            request = request,
        )

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
        val command: PoseQuery.GetScrapPoses = requestConverter.toGetPoseScrapCommand(
            userId = userId,
            page = page,
            size = size,
            sortOrder = sortOrder,
        )

        val result: PoseResult.GetPoses = getScrapPosesUseCase.execute(command)

        val response: GetPosesResponse = responseConverter.toGetPosesResponse(result)

        return BaseResponse(data = response)
    }
}
