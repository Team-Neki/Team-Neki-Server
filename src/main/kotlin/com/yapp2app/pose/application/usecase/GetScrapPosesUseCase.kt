package com.yapp2app.pose.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.pose.application.command.GetPosesCommand
import com.yapp2app.pose.application.port.MediaClientPort
import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.application.result.GetPosesResult
import com.yapp2app.pose.domain.entity.Pose

/**
 * fileName       : GetScrapPosesUseCase
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@UseCase
class GetScrapPosesUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: GetPosesCommand): GetPosesResult {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = command.size + 1

        val poses: List<Pose> = transactionRunner.readOnly {
            poseRepository.listPoses(
                offset = command.page * command.size,
                limit = fetchSize,
                headCount = command.headCount,
                sortOrder = command.sortOrder,
            )
        }
    }
}