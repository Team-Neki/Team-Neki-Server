package com.neki.media.models

import java.time.Instant

data class MediaStorageUploadTicket(
    val url: String,
    val method: String,
    val expiresAt: Instant,
    val contentType: String,
)
