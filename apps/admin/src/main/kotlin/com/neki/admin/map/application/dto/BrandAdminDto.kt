package com.neki.admin.map.application.dto

import java.time.LocalDate

/**
 * fileName       : BrandAdminDto
 * author         : koo
 * date           : 2026. 8. 9. 오전 1:02
 * description    :
 */
object BrandAdminDto {
    class Result {
        data class GetBrands(
            val brandName: String,
            val androidQr: Boolean,
            val iosQr: Boolean,
            val exposeToMap: Boolean,
            val updatedAt: LocalDate,
        )
    }
}
