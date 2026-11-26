package com.neki.domain.share.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.share.ShareTokenPolicy
import com.neki.domain.share.dto.ShareCommand
import com.neki.domain.share.dto.ShareQuery
import com.neki.domain.share.external.ShareTokenGenerator
import com.neki.domain.share.models.ShareLink
import com.neki.domain.share.models.SharePhoto
import com.neki.domain.share.repository.ShareLinkRepository
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

/**
 * fileName       : ShareService
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 링크 도메인 서비스. 발급·재사용과 공개 조회 판정(404/410)을 맡는다.
 *
 * | 상황                              | 결과              |
 * |-----------------------------------|-------------------|
 * | 토큰 형식 오류                    | SHARE_NOT_FOUND   |
 * | DB 에 없음 (soft-deleted 링크 포함) | SHARE_NOT_FOUND   |
 * | 만료                              | SHARE_EXPIRED     |
 * | 원본 사진 삭제 / media 비 UPLOADED | SHARE_EXPIRED     |
 *
 * soft-deleted 링크는 @SQLRestriction 때문에 조회되지 않아 404 가 된다. 링크를 soft delete 하는
 * 코드 경로(공유 취소 API)가 아직 없어서 우회 쿼리를 두지 않았다. 취소 API 를 추가할 때 410 이 필요하면
 * 삭제 포함 조회를 추가하고 이 판정을 바꾼다.
 */
@Component
class ShareService(
    private val shareLinkRepository: ShareLinkRepository,
    private val tokenGenerator: ShareTokenGenerator,
) {

    fun requireOwnedPhoto(photo: SharePhoto?): SharePhoto = photo ?: throw BusinessException(ResultCode.NOT_FOUND)

    fun findReusableLink(command: ShareCommand.IssueShareLink, now: LocalDateTime): ShareLink? =
        shareLinkRepository.getLatestAlive(command.userId, command.photoId, now)

    fun createLink(command: ShareCommand.IssueShareLink, now: LocalDateTime, expiration: Duration): ShareLink =
        shareLinkRepository.save(
            ShareLink.issue(
                token = tokenGenerator.generate(),
                userId = command.userId,
                photoId = command.photoId,
                now = now,
                validFor = expiration,
            ),
        )

    /** 404: 형식 오류, 미존재(soft-deleted 포함) / 410: 존재하지만 만료 */
    fun getViewableLink(query: ShareQuery.ViewSharedImage, now: LocalDateTime): ShareLink {
        if (!ShareTokenPolicy.isWellFormed(query.token)) {
            throw BusinessException(ResultCode.SHARE_NOT_FOUND)
        }

        val link: ShareLink = shareLinkRepository.getByToken(query.token)
            ?: throw BusinessException(ResultCode.SHARE_NOT_FOUND)

        if (link.isExpired(now)) {
            throw BusinessException(ResultCode.SHARE_EXPIRED)
        }

        return link
    }

    /** 링크는 유효하지만 원본 사진이 지워졌거나 media 가 UPLOADED 가 아니면 410 */
    fun requireViewablePhoto(photo: SharePhoto?): SharePhoto =
        photo ?: throw BusinessException(ResultCode.SHARE_EXPIRED)
}
