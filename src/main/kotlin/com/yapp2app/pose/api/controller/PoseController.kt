package com.yapp2app.pose.api.controller

import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.pose.api.converter.PoseCommandConverter
import com.yapp2app.pose.api.converter.dto.UploadPoseRequest
import com.yapp2app.pose.application.usecase.UploadPosesUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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

    private val commandConverter: PoseCommandConverter,
) {

    @Operation(
        summary = "포즈 등록 API",
        description = """
            ADMIN 권한이 있는 사용자만 호출이 가능합니다.
            관리자가 포즈를 업로드할 때 해당 API를 사용합니다.
            """,
    )
    @PostMapping
    fun uploadPoses(@Valid @RequestBody request: UploadPoseRequest): BaseResponse<Any> {
        // userId를 null로 지정하면 시스템이 올린 포즈로 간주
        val command = commandConverter.toUploadPoseCommand(null, request)

        uploadPosesUseCase.execute(command)

        return BaseResponse()
    }
}
