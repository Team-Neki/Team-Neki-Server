package com.neki.admin.pose.api

import com.neki.core.domain.vo.Pagination
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery

/**
 * fileName       : PoseAdminMapper
 * author         : koo
 * date           : 2026. 8. 10.
 * description    :
 */
fun PoseAdminDto.Request.GetPoses.toQuery(): PoseQuery.GetAllPoses =
    PoseQuery.GetAllPoses(headCount, Pagination(page, size))

/**
 * admin 은 인증이 없어 userId 로 귀속시킬 사용자가 없다.
 */
fun PoseAdminDto.Request.UploadPoses.toCommand(): PoseCommand.UploadPoses = PoseCommand.UploadPoses(
    userId = null,
    uploads = uploads.map { PoseCommand.UploadPoses.Item(it.mediaId!!, it.headCount, it.memo) },
)
