package com.neki.api.share.application

import com.neki.api.share.application.dto.ShareResult
import com.neki.api.share.config.ShareProperties
import com.neki.core.annotation.UseCase
import com.neki.domain.share.client.PhotoClient
import com.neki.domain.share.dto.ShareCommand
import com.neki.domain.share.models.ShareLink
import com.neki.domain.share.service.ShareService
import java.time.LocalDateTime

/**
 * fileName       : IssueShareLinkUseCase
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 사진 공유 링크 발급. 같은 사진에 살아있는 링크가 있으면 재사용한다.
 * - 동시 요청으로 링크가 두 개 생겨도 둘 다 유효하므로 별도 트랜잭션/락을 두지 않는다.
 */
@UseCase
class IssueShareLinkUseCase(
    private val shareService: ShareService,
    private val photoClient: PhotoClient,
    private val shareProperties: ShareProperties,
) {

    fun execute(command: ShareCommand.IssueShareLink): ShareResult.IssueShareLink {
        val now: LocalDateTime = LocalDateTime.now()

        shareService.requireOwnedPhoto(photoClient.findSharablePhoto(command.userId, command.photoId))

        val link: ShareLink = shareService.findReusableLink(command, now)
            ?: shareService.createLink(command, now, shareProperties.expiration)

        return ShareResult.IssueShareLink(token = link.token, expiresAt = link.expiresAt)
    }
}
