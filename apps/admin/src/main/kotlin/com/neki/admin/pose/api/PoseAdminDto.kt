package com.neki.admin.pose.api

import com.neki.core.domain.vo.CountedPage
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
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

        data class UploadPoses(val uploads: List<Item>) {
            data class Item(
                @field:NotNull(message = "mediaId는 필수 입력값입니다.")
                val mediaId: Long?,
                val headCount: HeadCount,
                val memo: String?,
            )
        }

        companion object {
            const val DEFAULT_PAGE = 0
            const val DEFAULT_SIZE = 10
            const val MAX_SIZE = 100L
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
                val viewCount: Long,
            )

            companion object {
                fun of(headCount: HeadCount?, page: CountedPage<Pose>): GetPoses = GetPoses(
                    headCount = headCount,
                    totalCount = page.totalCount,
                    totalPages = page.totalPages,
                    poses = page.items.map {
                        PoseInfo(
                            it.id!!,
                            it.mediaId,
                            it.headCount,
                            it.memo,
                            it.updatedAt!!,
                            it.viewCount,
                        )
                    },
                )
            }
        }
    }
}
