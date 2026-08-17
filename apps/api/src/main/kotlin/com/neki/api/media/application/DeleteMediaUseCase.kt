package com.neki.api.media.application

import com.neki.api.media.application.dto.MediaResult
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.models.Media
import com.neki.domain.media.service.MediaBinaryService
import com.neki.domain.media.service.MediaService

/**
 * fileName       : DeleteMediaUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 5:13
 * description    : media 삭제 usecase
 * - 단건 삭제도 mediaIds 를 한 건만 담아 호출한다.
 */
@UseCase
class DeleteMediaUseCase(
    private val mediaService: MediaService,
    private val mediaBinaryService: MediaBinaryService,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: MediaCommand.DeleteMedias): MediaResult.DeleteMedias {
        val medias: List<Media> = transactionRunner.run { mediaService.deleteMedias(command) }

        medias.forEach { mediaBinaryService.evict(it) }

        return MediaResult.DeleteMedias(medias.map { it.id!! })
    }
}
