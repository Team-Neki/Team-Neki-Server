package com.neki.admin.pose.api.controller

import com.neki.admin.pose.api.dto.PoseAdminConverter
import com.neki.admin.pose.api.dto.PoseAdminDto
import com.neki.admin.pose.application.PoseAdminFacade
import com.neki.admin.pose.application.dto.PoseAdminResult
import com.neki.core.api.dto.BaseResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : PoseAdminController
 * author         : koo
 * date           : 2026. 8. 9. 오후 11:40
 * description    :
 */
@RestController
@RequestMapping("/api/pose")
class PoseAdminController(private val poseAdminFacade: PoseAdminFacade) {

    @GetMapping
    fun getPoses(@Valid request: PoseAdminDto.Request.GetPoses): BaseResponse<PoseAdminDto.Response.GetPoses> {
        val result: PoseAdminResult.GetPoses = poseAdminFacade.getPoses(PoseAdminConverter.toQuery(request))
        return BaseResponse(data = PoseAdminConverter.toResponse(result))
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun uploadPoses(@RequestBody @Valid request: PoseAdminDto.Request.UploadPoses): BaseResponse<Any> {
        poseAdminFacade.uploadPoses(PoseAdminConverter.toCommand(request))
        return BaseResponse(data = null)
    }

    @PatchMapping("/{poseId}")
    fun updatePoseMedia(
        @RequestBody @Valid request: PoseAdminDto.Request.UpdatePoseMedia,
        @PathVariable poseId: Long,
    ): BaseResponse<Any> {
        poseAdminFacade.updatePoseMedia(PoseAdminConverter.toCommand(poseId, request))
        return BaseResponse(data = null)
    }
}
