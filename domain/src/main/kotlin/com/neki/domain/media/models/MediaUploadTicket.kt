package com.neki.domain.media.models

/**
 * fileName       : MediaUploadTicket
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : 발급된 업로드 티켓과 대상 미디어 쌍
 */
data class MediaUploadTicket(val media: Media, val ticket: MediaStorageUploadTicket)
