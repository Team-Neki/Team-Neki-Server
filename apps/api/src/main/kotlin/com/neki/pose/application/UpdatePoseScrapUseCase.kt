package com.neki.pose.application

import com.neki.common.annotation.UseCase
import com.neki.pose.dto.PoseCommand
import com.neki.pose.service.PoseService
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdatePoseScrapUseCase(private val poseService: PoseService) {

    @Transactional
    fun execute(command: PoseCommand.UpdatePoseScrap) = poseService.updateScrap(command)
}
