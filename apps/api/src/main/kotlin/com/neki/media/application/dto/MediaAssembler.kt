package com.neki.media.application.dto

import com.neki.media.models.Media
import com.neki.media.models.MediaUploadTickets

/**
 * fileName       : MediaAssembler
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : media 엔티티를 응답 항목으로 조립한다.
 */
object MediaAssembler {

    fun toMetadata(media: Media): MediaResult.Metadata = MediaResult.Metadata(
        mediaId = media.id!!,
        storageKey = media.storageKey,
        contentType = media.contentType,
        width = media.width,
        height = media.height,
    )

    fun toMetadatas(medias: List<Media>): List<MediaResult.Metadata> = medias.map { toMetadata(it) }

    /**
     * 바이너리는 media별로 따로 조회해 넘겨받는다 (조회는 유스케이스가 담당).
     */
    fun toBinaries(medias: List<Media>, binaryByMediaId: Map<Long, ByteArray>): List<MediaResult.Binary> =
        medias.map { media ->
            MediaResult.Binary(
                media = toMetadata(media),
                binaryData = binaryByMediaId.getValue(media.id!!),
            )
        }

    fun toUploadTicketItems(issuedTickets: MediaUploadTickets): List<MediaResult.GenerateUploadTicket.Item> =
        issuedTickets.tickets.map {
            MediaResult.GenerateUploadTicket.Item(
                mediaId = it.media.id!!,
                uploadUrl = it.ticket.url,
                contentType = it.media.contentType,
            )
        }
}
