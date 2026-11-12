package com.neki.admin.pose.api.dto

import com.neki.admin.pose.application.dto.PoseAdminResult
import com.neki.core.domain.vo.Pagination
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery

object PoseAdminConverter {
    fun toQuery(request: PoseAdminDto.Request.GetPoses): PoseQuery.GetAllPoses =
        PoseQuery.GetAllPoses(request.headCount, Pagination(request.page, request.size))

    /**
     * 공동 관리 포즈라 특정 사용자에게 귀속시키지 않는다. 업로더는 TB_MEDIA.owner_id 에 남는다.
     */
    fun toCommand(request: PoseAdminDto.Request.UploadPoses): PoseCommand.UploadPoses = PoseCommand.UploadPoses(
        userId = null,
        uploads = request.uploads.map { PoseCommand.UploadPoses.Item(it.mediaId!!, it.headCount, it.memo) },
    )

    fun toCommand(poseId: Long, request: PoseAdminDto.Request.UpdatePoseMedia): PoseCommand.UpdatePoseMedia =
        PoseCommand.UpdatePoseMedia(poseId, request.mediaId!!)

    fun toResponse(result: PoseAdminResult.GetPoses): PoseAdminDto.Response.GetPoses = PoseAdminDto.Response.GetPoses(
        headCount = result.headCount,
        totalCount = result.totalCount,
        totalPages = result.totalPages,
        poses = result.poses.map {
            PoseAdminDto.Response.GetPoses.PoseInfo(it.id, it.mediaId, it.headCount, it.memo, it.updatedAt)
        },
    )
}
