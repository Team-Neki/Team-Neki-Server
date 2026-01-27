package com.yapp2app.pose.infra.client

import com.yapp2app.media.application.command.ConfirmMediaUploadedCommand
import com.yapp2app.media.application.result.ConfirmMediaUploadedResult.UploadConfirmStatus
import com.yapp2app.media.application.usecase.ConfirmMediaUploadedUseCase
import com.yapp2app.photo.application.contract.MediaAvailability
import com.yapp2app.pose.application.port.MediaClientPort
import org.springframework.stereotype.Component

/**
 * fileName       : PostMediaClient
 * author         : darren
 * date           : 2026. 1. 27. 17:20
 * description    : monolithic architecture media client
 * - media service 분리 시 OpenFeign, EventPublisher/Consumer로 변경
 */
@Component
class PoseMediaClient(private val confirmMediaUploadedUseCase: ConfirmMediaUploadedUseCase) : MediaClientPort {
    override fun verifyMediasUploaded(ownerId: Long?, mediaIds: List<Long>): Map<Long, MediaAvailability> {
        if (mediaIds.isEmpty()) return emptyMap()

        val result = confirmMediaUploadedUseCase.execute(
            ConfirmMediaUploadedCommand(ownerId = ownerId, mediaIds = mediaIds),
        )
        return result.results.mapValues { (_, status) ->
            if (status == UploadConfirmStatus.CONFIRMED) MediaAvailability.AVAILABLE else MediaAvailability.UNAVAILABLE
        }
    }

    override fun rollbackMediasUploaded(ownerId: Long?, mediaIds: List<Long>) {
        if (mediaIds.isEmpty()) return

        confirmMediaUploadedUseCase.rollback(
            ConfirmMediaUploadedCommand(
                ownerId = ownerId,
                mediaIds = mediaIds,
            ),
        )
    }
}
