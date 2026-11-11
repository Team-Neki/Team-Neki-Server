package com.neki.admin.media.application.dto

import java.time.Instant

object AdminMediaResult {
    data class UploadTicket(val method: String, val expiresAt: Instant, val items: List<Item>) {
        data class Item(val mediaId: Long, val uploadUrl: String, val contentType: String)
    }
}
