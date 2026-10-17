package com.neki.domain.map.dto

/**
 * fileName       : BrandCommand
 * author         : koo
 * date           : 2026. 8. 9. 오전 1:13
 * description    :
 */
object BrandCommand {
    /**
     * brandId 를 뺀 나머지는 null 이면 변경하지 않는다.
     */
    data class UpdateBrand(
        val brandId: Long,
        val name: String?,
        val code: String?,
        val mediaId: Long?,
        val supportAndroidQr: Boolean?,
        val supportIosQr: Boolean?,
        val exposeToMap: Boolean?,
    ) {
        val hasNoChanges: Boolean
            get() = listOf(name, code, mediaId, supportAndroidQr, supportIosQr, exposeToMap).all { it == null }
    }

    data class DeleteBrand(val brandId: Long)

    data class AddBrand(
        val name: String,
        val code: String,
        val mediaId: Long,
        val supportAndroidQr: Boolean,
        val supportIosQr: Boolean,
    )
}
