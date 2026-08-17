package com.neki.api.pose.application

import com.neki.core.annotation.UseCase
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.service.PoseService
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdatePoseScrapUseCase(private val poseService: PoseService) {

    @Transactional
    fun execute(command: PoseCommand.UpdatePoseScrap) = poseService.updateScrap(command)
}
