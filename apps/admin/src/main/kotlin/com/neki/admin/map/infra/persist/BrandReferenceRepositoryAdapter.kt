package com.neki.admin.map.infra.persist

import com.neki.domain.map.models.Brand
import com.neki.domain.map.repository.BrandReferenceRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * fileName       : BrandReferenceRepositoryAdapter
 * description    : Workflow·검색 색인이 소유한 테이블이라 엔티티 없이 존재 여부만 읽는다.
 * 즐겨찾기(tb_favorite_photo_booth)는 지점 마스터와 조인해야 보이므로, 지점 참조가 없으면 남아 있어도 노출되지 않아 검사하지 않는다.
 */
@Repository
class BrandReferenceRepositoryAdapter(private val jdbc: JdbcTemplate) : BrandReferenceRepository {

    override fun isReferenced(brand: Brand): Boolean =
        exists("SELECT EXISTS (SELECT 1 FROM tb_photo_booth_enriched WHERE platform = ?)", brand.code) ||
            exists(
                "SELECT EXISTS (SELECT 1 FROM tb_photo_booth_location WHERE brand_id = ? OR source_platform = ?)",
                brand.id,
                brand.code,
            ) ||
            exists(
                "SELECT EXISTS (SELECT 1 FROM tb_photo_booth_manual WHERE brand_id = ? AND deleted_at IS NULL)",
                brand.id,
            ) ||
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

    private fun exists(sql: String, vararg values: Any?): Boolean =
        jdbc.queryForObject(sql, Boolean::class.java, *values) == true
}
