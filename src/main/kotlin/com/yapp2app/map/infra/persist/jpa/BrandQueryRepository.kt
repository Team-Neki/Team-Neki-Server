package com.yapp2app.map.infra.persist.jpa

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.map.application.contract.BrandDto
import com.yapp2app.map.domain.entity.QBrand.brand
import com.yapp2app.media.domain.entity.QMedia.media
import org.springframework.stereotype.Repository

/**
 * fileName       : BrandQueryRepository
 * author         : darren
 * date           : 2026. 1. 21. 14:50
 * description    : Brand QueryDsl Repository
 */
@Repository
class BrandQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findAll(): List<BrandDto> = queryFactory
        .select(
            Projections.constructor(
                BrandDto::class.java,
                brand.id,
                brand.name,
                brand.code,
                media.storageKey,
            ),
        )
        .from(brand)
        .leftJoin(media).on(media.id.eq(brand.mediaId))
        .fetch()
}
