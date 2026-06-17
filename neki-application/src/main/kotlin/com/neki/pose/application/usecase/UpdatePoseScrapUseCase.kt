package com.neki.pose.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.command.UpdatePoseScrapCommand
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.ScrapPoseRepositoryPort
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdatePoseScrapUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val scrapPoseRepository: ScrapPoseRepositoryPort,
) {

    @Transactional
    fun execute(command: UpdatePoseScrapCommand) {
        val poseExists: Boolean =
            poseRepository.existsPose(command.poseId)

        if (!poseExists) throw BusinessException(ResultCode.NOT_FOUND)

        if (command.scrap) {
            scrapPoseRepository.add(command.userId, command.poseId)
        } else {
            scrapPoseRepository.delete(command.userId, command.poseId)
        }
    }
}
