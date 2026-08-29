package com.neki.admin.pose.application

import com.neki.admin.pose.api.PoseAdminDto
import com.neki.core.domain.vo.CountedPage
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.service.PoseService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : PoseAdminFacade
 * author         : koo
 * date           : 2026. 8. 9. 오후 11:44
 * description    :
 */
@Service
@Transactional(readOnly = true)
class PoseAdminFacade(private val poseService: PoseService) {

    fun getPoses(query: PoseQuery.GetAllPoses): PoseAdminDto.Response.GetPoses {
        val poses: CountedPage<Pose> = poseService.listAllPoses(query)
        return PoseAdminDto.Response.GetPoses.of(query.headCount, poses)
    }

    @Transactional
    fun uploadPoses(command: PoseCommand.UploadPoses) {
        val poses: List<Pose> = poseService.createPoses(command)
        poseService.saveAll(poses)
    }

    @Transactional
    fun updatePoseMedia(command: PoseCommand.UpdatePoseMedia) {
        poseService.updatePoseMedia(command)
    }
}
