package com.neki.admin.media.api.dto

import com.neki.admin.media.application.dto.AdminMediaResult
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.models.MediaType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant

object AdminMediaDto {
    data class UploadRequest(
        @field:Valid
        @field:NotEmpty
        @field:Size(max = 10)
        val items: List<Item>,
    ) {
        data class Item(
            @field:NotNull val mediaType: MediaType?,
            @field:NotBlank val contentType: String?,
            @field:Positive val width: Int? = null,
            @field:Positive val height: Int? = null,
            @field:Positive val size: Long? = null,
        )
    }

    data class UploadResponse(val method: String, val expiresAt: Instant, val items: List<Item>) {
        data class Item(val mediaId: Long, val uploadTicket: String, val contentType: String)
    }
}

object AdminMediaConverter {
    fun toCommand(uploaderId: Long, request: AdminMediaDto.UploadRequest): MediaCommand.GenerateUploadTicket =
        MediaCommand.GenerateUploadTicket(
            ownerId = uploaderId,
            items = request.items.map { item ->
                MediaCommand.GenerateUploadTicket.Item(
                    filename = null,
                    contentType = item.contentType!!,
                    mediaType = item.mediaType!!,
                    width = item.width,
                    height = item.height,
                    size = item.size,
                )
            },
            adminManaged = true,
        )

    fun toResponse(result: AdminMediaResult.UploadTicket): AdminMediaDto.UploadResponse = AdminMediaDto.UploadResponse(
        method = result.method,
        expiresAt = result.expiresAt,
        items = result.items.map { AdminMediaDto.UploadResponse.Item(it.mediaId, it.uploadUrl, it.contentType) },
    )
}
