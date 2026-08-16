package com.neki.admin.pose.api

import com.neki.admin.pose.application.PoseAdminFacade
import com.neki.core.api.dto.BaseResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
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
@RequestMapping("/admin/v1/pose")
class PoseAdminController(private val poseAdminFacade: PoseAdminFacade) {

    @GetMapping
    fun getPoses(@Valid request: PoseAdminDto.Request.GetPoses): BaseResponse<PoseAdminDto.Response.GetPoses> =
        BaseResponse(data = poseAdminFacade.getPoses(request.toQuery()))

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun uploadPoses(@RequestBody @Valid request: PoseAdminDto.Request.UploadPoses): BaseResponse<Any> {
        poseAdminFacade.uploadPoses(request.toCommand())
        return BaseResponse(data = null)
    }
}
