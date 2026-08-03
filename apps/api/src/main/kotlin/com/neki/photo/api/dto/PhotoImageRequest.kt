package com.neki.photo.api.dto

import com.neki.photo.models.UploadMethod
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.annotation.Nullable
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * fileName       : PhotoImageRequest
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Photo image 관련 요청 DTO
 */
object PhotoImageRequest {
    @Schema(name = "UploadPhotoRequest")
    data class UploadPhoto(
        @field:Nullable
        val folderId: Long?,

        @field:NotEmpty(message = "uploads가 비어있습니다.")
        @field:Valid
        @field:Size(max = 10, message = "한 번에 최대 10장까지 업로드할 수 있습니다.")
        val uploads: List<Item>,

        @field:Schema(description = "업로드 사진 즐겨찾기 등록 여부", example = "true")
        val favorite: Boolean? = null,
    ) {
        @Schema(name = "UploadPhotoItem")
        data class Item(
            @field:NotNull(message = "mediaId는 필수 입력값입니다.")
            val mediaId: Long?,

            @field:Schema(
                description = "업로드 방식",
                example = "QR",
                allowableValues = ["QR", "DIRECT_UPLOAD"],
            )
            val uploadMethod: UploadMethod? = null,

            @field:Schema(description = "메모", example = "대학교 친구들이랑")
            val memo: String?,

            @field:Schema(description = "사진 찍은 날짜", example = "2025-12-23T07:09:00")
            val capturedAt: LocalDateTime?,
        )
    }

    @Schema(name = "DeletePhotosRequest")
    data class DeletePhotos(
        @field:NotEmpty(message = "photoIds가 비어있습니다.")
        val photoIds: List<Long>,
    )

    @Schema(name = "UpdatePhotoRequest")
    data class UpdatePhoto(
        @field:Schema(description = "메모", example = "수정된 메모")
        val memo: String?,

        @field:Schema(description = "사진 찍은 날짜", example = "2025-12-23T07:09:00")
        val capturedAt: LocalDateTime?,
    )

    @Schema(name = "UpdatePhotoFavoriteRequest")
    data class UpdatePhotoFavorite(
        @field:Schema(description = "변경하고자 하는 즐겨찾기 상태", example = "true")
        @field:NotNull(message = "favorite은 필수값입니다.")
        val favorite: Boolean?,
    )
}
