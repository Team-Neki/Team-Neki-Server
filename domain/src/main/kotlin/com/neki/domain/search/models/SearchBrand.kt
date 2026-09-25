package com.neki.domain.search.models

/**
 * fileName       : SearchBrand
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 색인이 보는 브랜드. platform 은 tb_photo_booth_enriched.platform 과 조인하는 키이며 수집하지 않는 브랜드는 null
 */
data class SearchBrand(val id: Long, val name: String, val code: String, val platform: String?)
