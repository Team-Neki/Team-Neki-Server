package com.neki.admin.map.api

import com.neki.core.domain.vo.CountedPage
import com.neki.domain.map.models.Brand
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * fileName       : BrandAdminDto
 * author         : koo
 * date           : 2026. 8. 8. 오후 6:30
 * description    :
 */
object BrandAdminDto {

    class Request {
        data class GetBrands(
            val supportsQr: Boolean? = null,
            val exposeToMap: Boolean? = null,
            @field:Min(value = 0, message = "page는 0 이상이어야 합니다.")
            val page: Int = DEFAULT_PAGE,
            @field:Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @field:Max(value = MAX_SIZE, message = "size는 {value} 이하여야 합니다.")
            val size: Int = DEFAULT_SIZE,
        )

        data class SearchBrands(
            @field:NotBlank(message = "keyword는 필수 입력값입니다.")
            val keyword: String,
            @field:Min(value = 0, message = "page는 0 이상이어야 합니다.")
            val page: Int = DEFAULT_PAGE,
            @field:Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @field:Max(value = MAX_SIZE, message = "size는 {value} 이하여야 합니다.")
            val size: Int = DEFAULT_SIZE,
        )

        /**
         * 넘기지 않은 필드는 변경하지 않는다. 하나도 넘기지 않으면 400 이다.
         */
        data class UpdateBrand(
            @field:Size(min = 1, max = 50, message = "name은 1자 이상 50자 이하여야 합니다.")
            val name: String? = null,
            @field:Size(min = 1, max = 30, message = "code는 1자 이상 30자 이하여야 합니다.")
            val code: String? = null,
            val supportAndroidQr: Boolean? = null,
            val supportIosQr: Boolean? = null,
            val exposeToMap: Boolean? = null,
        )

        data class AddBrand(
            @field:Size(min = 1, max = 50, message = "name은 1자 이상 50자 이하여야 합니다.")
            val name: String,
            @field:Size(min = 1, max = 30, message = "code는 1자 이상 30자 이하여야 합니다.")
            val code: String,
            @field:NotNull(message = "mediaId는 필수 입력값입니다.")
            val mediaId: Long?,
            val supportAndroidQr: Boolean,
            val supportIosQr: Boolean,
            val exposeToMap: Boolean,
        )

        companion object {
            const val DEFAULT_PAGE = 0
            const val DEFAULT_SIZE = 10
            const val MAX_SIZE = 100L
        }
    }

    class Response {
        /**
         * brandCounts 는 현재 페이지가 아니라 조건에 맞는 전체 브랜드 수다.
         */
        data class GetBrands(val brandCounts: Long, val totalPages: Int, val brandInfos: List<BrandInfo>) {
            data class BrandInfo(
                val name: String,
                val supportAndroidQr: Boolean,
                val supportIosQr: Boolean,
                val exposeToMap: Boolean,
                val updatedAt: LocalDate,
            )

            companion object {
                fun of(page: CountedPage<Brand>): GetBrands = GetBrands(
                    brandCounts = page.totalCount,
                    totalPages = page.totalPages,
                    brandInfos = page.items.map {
                        BrandInfo(
                            it.name,
                            it.supportAndroidQr,
                            it.supportIosQr,
                            it.exposeToMap,
                            it.updatedAt!!.toLocalDate(),
                        )
                    },
                )
            }
        }
    }
}
