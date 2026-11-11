package com.neki.admin.pose.api.dto

import com.neki.domain.pose.models.HeadCount
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * fileName       : PoseAdminDto
 * author         : koo
 * date           : 2026. 8. 9. 오후 11:40
 * description    :
 */
object PoseAdminDto {
    class Request {
        /**
         * headCount 는 넘기지 않으면 인원수로 거르지 않는다.
         */
        data class GetPoses(
            val headCount: HeadCount? = null,
            @field:Min(value = 0, message = "page는 0 이상이어야 합니다.")
            val page: Int = DEFAULT_PAGE,
            @field:Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @field:Max(value = MAX_SIZE, message = "size는 {value} 이하여야 합니다.")
            val size: Int = DEFAULT_SIZE,
        )

        /**
         * 미디어마다 S3 객체를 확인하는 동안 트랜잭션을 잡고 있으므로 한 번에 올리는 건수를 업로드 티켓 발급 상한과 같게 제한한다.
         */
        data class UploadPoses(
            @field:NotEmpty
            @field:Size(max = MAX_UPLOADS, message = "uploads는 {max}개 이하여야 합니다.")
            @field:Valid
            val uploads: List<Item>,
        ) {
            data class Item(
                @field:NotNull(message = "mediaId는 필수 입력값입니다.")
                val mediaId: Long?,
                val headCount: HeadCount,
                @field:Size(max = 255, message = "memo는 255자 이하여야 합니다.")
                val memo: String?,
            )
        }

        data class UpdatePoseMedia(
            @field:NotNull(message = "mediaId는 필수 입력값입니다.")
            val mediaId: Long?,
        )

        companion object {
            const val DEFAULT_PAGE = 0
            const val DEFAULT_SIZE = 10
            const val MAX_SIZE = 100L
            const val MAX_UPLOADS = 10
        }
    }

    class Response {
        /**
         * totalCount 는 현재 페이지가 아니라 조건에 맞는 전체 포즈 수다.
         */
        data class GetPoses(
            val headCount: HeadCount?,
            val totalCount: Long,
            val totalPages: Int,
            val poses: List<PoseInfo>,
        ) {
            data class PoseInfo(
                val id: Long,
                val mediaId: Long,
                val headCount: HeadCount,
                val memo: String?,
                val updatedAt: LocalDateTime,
            )
        }
    }
}
