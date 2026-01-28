package com.yapp2app.pose.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.pose.application.command.UpdatePoseScrapCommand
import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.application.port.ScrapPoseRepositoryPort
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdatePoseScrapUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val scrapPoseRepository: ScrapPoseRepositoryPort,
) {

    @Transactional
    fun execute(command: UpdatePoseScrapCommand) {
        val poseExists: Boolean =
            poseRepository.existsOwnedPose(command.userId, command.poseId)

        if (!poseExists) throw BusinessException(ResultCode.NOT_FOUND)

        if (command.scrap) {
            scrapPoseRepository.add(command.userId, command.poseId)
        } else {
            scrapPoseRepository.delete(command.userId, command.poseId)
        }
    }
}
