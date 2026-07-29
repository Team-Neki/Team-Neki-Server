package com.neki.pose.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.dto.PoseCommand
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.ScrapPoseRepositoryPort
import com.neki.pose.domain.entity.ScrapPose
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdatePoseScrapUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val scrapPoseRepository: ScrapPoseRepositoryPort,
) {

    @Transactional
    fun execute(command: PoseCommand.UpdatePoseScrap) {
        val poseExists: Boolean =
            poseRepository.existsPose(command.poseId)

        if (!poseExists) throw BusinessException(ResultCode.NOT_FOUND)

        val scrapPose = ScrapPose(command.userId, command.poseId)
        if (command.scrap) {
            scrapPoseRepository.add(scrapPose)
        } else {
            scrapPoseRepository.delete(scrapPose)
        }
    }
}
