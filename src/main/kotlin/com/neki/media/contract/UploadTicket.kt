package com.neki.media.contract

import java.time.Instant

/**
 * fileName       : UploadTicket
 * author         : koo
 * date           : 2026. 1. 24. 오후 2:55
 * description    :
 */
data class UploadTicket(val url: String, val method: String, val expiresAt: Instant, val contentType: String)
