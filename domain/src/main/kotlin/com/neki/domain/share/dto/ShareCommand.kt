package com.neki.domain.share.dto

/**
 * fileName       : ShareCommand
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : Share domain command
 */
object ShareCommand {
    data class IssueShareLink(val userId: Long, val photoId: Long)
}
