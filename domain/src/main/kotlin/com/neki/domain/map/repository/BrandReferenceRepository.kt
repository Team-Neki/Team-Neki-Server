package com.neki.domain.map.repository

import com.neki.domain.map.models.Brand

/**
 * fileName       : BrandReferenceRepository
 * description    : 브랜드를 원천 키(code)나 ID로 참조하는 지점·검색 색인이 남아 있는지 확인한다
 */
interface BrandReferenceRepository {

    fun isReferenced(brand: Brand): Boolean
}
