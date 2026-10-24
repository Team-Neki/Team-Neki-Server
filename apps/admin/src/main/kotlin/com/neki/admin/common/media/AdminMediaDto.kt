package com.neki.admin.common.media

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
