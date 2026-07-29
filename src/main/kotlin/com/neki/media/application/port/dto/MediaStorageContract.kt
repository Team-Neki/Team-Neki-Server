package com.neki.media.application.port.dto

import java.time.Instant

/**
 * fileName       : MediaStorageContract
 * author         : koo
 * date           : 2026. 7. 22.
 * description    : MediaStoragePort 계약 타입
 */
object MediaStorageContract {
    data class UploadTicket(val url: String, val method: String, val expiresAt: Instant, val contentType: String)
}
