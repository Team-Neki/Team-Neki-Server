package com.neki.admin.pose.application

import com.neki.admin.media.application.AdminMediaVerifier
import com.neki.admin.pose.application.dto.PoseAdminResult
import com.neki.core.domain.vo.CountedPage
import com.neki.domain.media.models.MediaType
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
class PoseAdminFacade(private val poseService: PoseService, private val mediaVerifier: AdminMediaVerifier) {

    fun getPoses(query: PoseQuery.GetAllPoses): PoseAdminResult.GetPoses {
        val page: CountedPage<Pose> = poseService.listAllPoses(query)
        return PoseAdminResult.GetPoses(
            headCount = query.headCount,
            totalCount = page.totalCount,
            totalPages = page.totalPages,
            poses = page.items.map {
                PoseAdminResult.GetPoses.PoseInfo(it.id!!, it.mediaId, it.headCount, it.memo, it.updatedAt!!)
            },
        )
    }

    @Transactional
    fun uploadPoses(command: PoseCommand.UploadPoses) {
        val poses: List<Pose> = poseService.createPoses(command)
        poseService.validateMediaNotInUse(command.uploads.map { it.mediaId })
        poseService.saveAll(poses)
        mediaVerifier.verifyAndConfirm(command.uploads.map { it.mediaId }, MediaType.POSE)
    }

    @Transactional
    fun updatePoseMedia(command: PoseCommand.UpdatePoseMedia) {
        poseService.updatePoseMedia(command)
        mediaVerifier.verifyAndConfirm(listOf(command.mediaId), MediaType.POSE)
    }
}
