package com.neki.media.models

/**
 * fileName       : MediaUploadTickets
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : 한 번의 발급 요청으로 만들어진 업로드 티켓 묶음
 */
data class MediaUploadTickets(val tickets: List<MediaUploadTicket>) {

    /**
     * method, expiresAt 은 한 번의 발급 요청 안에서 동일하므로 첫 티켓을 대표값으로 사용한다.
     */
    fun firstTicket(): MediaStorageUploadTicket = tickets.first().ticket
}
