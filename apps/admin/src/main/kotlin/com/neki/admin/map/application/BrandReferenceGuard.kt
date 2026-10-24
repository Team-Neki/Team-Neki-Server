package com.neki.admin.map.application

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.models.Brand
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class BrandReferenceGuard(private val jdbc: JdbcTemplate) {
    fun requireUnreferenced(brand: Brand) {
        if (exists("SELECT EXISTS (SELECT 1 FROM tb_photo_booth_enriched WHERE platform = ?)", brand.code) ||
            exists(
                "SELECT EXISTS (SELECT 1 FROM TB_PHOTO_BOOTH_LOCATION WHERE brand_id = ? OR source_platform = ?)",
                brand.id,
                brand.code,
            ) ||
            exists("SELECT EXISTS (SELECT 1 FROM TB_PHOTO_BOOTH_MANUAL WHERE brand_id = ?)", brand.id) ||
            exists("SELECT EXISTS (SELECT 1 FROM TB_FAVORITE_PHOTO_BOOTH WHERE platform = ?)", brand.code) ||
            exists(
                "SELECT EXISTS (SELECT 1 FROM tb_photo_booth_search_read WHERE brand_id = ? OR platform = ?)",
                brand.id,
                brand.code,
            ) ||
            exists(
                "SELECT EXISTS (SELECT 1 FROM tb_photo_booth_search_write WHERE brand_id = ? OR platform = ?)",
                brand.id,
                brand.code,
            )
        ) {
            throw BusinessException(ResultCode.CONFLICT_BRAND)
        }
    }

    private fun exists(sql: String, vararg values: Any?): Boolean =
        jdbc.queryForObject(sql, Boolean::class.java, *values) == true
}
