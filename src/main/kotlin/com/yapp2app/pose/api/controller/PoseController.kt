package com.yapp2app.pose.api.controller

import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.api.converter.PoseCommandConverter
import com.yapp2app.pose.api.converter.PoseResultConverter
import com.yapp2app.pose.api.dto.GetPoseResponse
import com.yapp2app.pose.api.dto.GetPosesResponse
import com.yapp2app.pose.api.dto.UploadPoseRequest
import com.yapp2app.pose.application.usecase.GetPoseUseCase
import com.yapp2app.pose.application.usecase.GetPosesUseCase
import com.yapp2app.pose.application.usecase.UploadPosesUseCase
import com.yapp2app.pose.domain.HeadCount
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : PoseController
 * author         : darren
 * date           : 2026. 1. 27. 17:46
 * description    : Pose api endpoint
 */
@RequiresSecurity
@Tag(name = "pose image", description = "포즈 사진 API")
@RestController
@RequestMapping("/api/poses")
class PoseController(
    private val uploadPosesUseCase: UploadPosesUseCase,
    private val getPosesUseCase: GetPosesUseCase,
    private val getPoseUseCase: GetPoseUseCase,

    private val commandConverter: PoseCommandConverter,
    private val resultConverter: PoseResultConverter,
) {

    @Operation(
        summary = "포즈 등록 API",
        description = """
            ADMIN 권한이 있는 사용자만 호출이 가능합니다.
            관리자가 포즈를 업로드할 때 해당 API를 사용합니다.
            """,
    )
    @PostMapping
    fun uploadPoses(
        @AuthenticationPrincipal(expression = "id") ownerId: Long,
        @Valid @RequestBody request: UploadPoseRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toUploadPosesCommand(ownerId, request)

        uploadPosesUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "포즈 목록 API",
        description = """
            포즈 목록을 조회합니다. Offset 기반 페이징을 지원합니다.

            headCount:
            * 없이 보내면 전체 조회
            * ONE("1인")
            * TWO("2인")
            * THREE("3인")
            * FOUR("4인")
            * FIVE_OR_MORE("5인 이상")
        """,
    )
    @GetMapping
    fun poseList(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) headCount: HeadCount?,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
    ): BaseResponse<GetPosesResponse> {
        val command = commandConverter.toGetPosesCommand(
            page = page,
            size = size,
            headCount = headCount,
            sortOrder = sortOrder,
        )

        val result = getPosesUseCase.execute(command)

        val response = resultConverter.toGetPosesResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "포즈 상세 조회 API",
        description = "포즈 상세 정보를 조회합니다.",
    )
    @GetMapping("/{poseId}")
    fun poseDetail(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable poseId: Long,
    ): BaseResponse<GetPoseResponse> {
        val command = commandConverter.toGetPoseCommand(userId, poseId)

        val result = getPoseUseCase.execute(command)

        val response = resultConverter.toGetPoseResponse(result)

        return BaseResponse(data = response)
    }
}
