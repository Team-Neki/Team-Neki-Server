package com.yapp2app.user.infra.client

import com.yapp2app.media.application.command.DeleteMediaCommand
import com.yapp2app.media.application.command.VerifyMediaOwnershipCommand
import com.yapp2app.media.application.usecase.DeleteMediaUseCase
import com.yapp2app.media.application.usecase.VerifyMediaOwnershipUseCase
import com.yapp2app.user.application.port.MediaClientPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * fileName       : UserMediaClient
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:59
 * description    :
 */
@Component
class UserMediaClient(
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val verifyMediaOwnershipUseCase: VerifyMediaOwnershipUseCase,
) : MediaClientPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun verifyMediaOwned(ownerId: Long, mediaId: Long) {
        verifyMediaOwnershipUseCase.execute(VerifyMediaOwnershipCommand(ownerId, mediaId))
    }

    override fun deleteMedia(ownerId: Long, mediaId: Long) {
        runCatching {
            deleteMediaUseCase.execute(DeleteMediaCommand(ownerId, mediaId))
        }.onFailure { e ->
            log.warn(
                "Failed to request media deletion. Will be cleaned up by batch later. ownerId={}, mediaId={}",
                ownerId,
                mediaId,
                e,
            )
        }
    }
}
