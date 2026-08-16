package com.neki.admin.map.infra.persist.jpa

import com.neki.core.domain.vo.Pagination
import com.neki.domain.map.dto.BrandQuery
import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.QBrand.brand
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : BrandQueryRepository
 * author         : koo
 * date           : 2026. 8. 9.
 * description    : 어드민 브랜드 목록·검색 조회용 QueryDSL repository
 */
@Repository
class BrandQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findAll(query: BrandQuery.GetBrands): List<Brand> = queryFactory
        .selectFrom(brand)
        .where(*conditions(query))
        .paginate(query.pagination)
        .fetch()

    fun count(query: BrandQuery.GetBrands): Long = queryFactory
        .select(brand.count())
        .from(brand)
        .where(*conditions(query))
        .fetchOne() ?: 0L

    fun findByKeyword(query: BrandQuery.SearchBrands): List<Brand> = queryFactory
        .selectFrom(brand)
        .where(*conditions(query))
        .paginate(query.pagination)
        .fetch()

    fun countByKeyword(query: BrandQuery.SearchBrands): Long = queryFactory
        .select(brand.count())
        .from(brand)
        .where(*conditions(query))
        .fetchOne() ?: 0L

    private fun conditions(query: BrandQuery.GetBrands): Array<Predicate?> = arrayOf(
        notDeleted,
        query.supportsQr?.let { supportsQr(it) },
        query.exposeToMap?.let { brand.exposeToMap.eq(it) },
    )

    private fun conditions(query: BrandQuery.SearchBrands): Array<Predicate?> = arrayOf(
        notDeleted,
        matchesKeyword(query.keyword),
    )

    private val notDeleted: BooleanExpression
        get() = brand.isDeleted.eq(false)

    // 안드로이드·iOS 중 하나라도 지원하면 QR 지원으로 본다
    private fun supportsQr(supportsQr: Boolean): BooleanExpression = if (supportsQr) {
        brand.supportAndroidQr.eq(true).or(brand.supportIosQr.eq(true))
    } else {
        brand.supportAndroidQr.eq(false).and(brand.supportIosQr.eq(false))
    }

    // 이름과 코드 중 하나라도 부분일치하면 검색된다
    private fun matchesKeyword(keyword: String): BooleanExpression =
        brand.name.containsIgnoreCase(keyword).or(brand.code.containsIgnoreCase(keyword))

    /**
     * 어드민 목록은 이름 내림차순 고정이라 Pagination.sortOrder 는 쓰지 않는다.
     * 전체 건수를 따로 세므로 hasNext 판단용 초과 조회(Pagination.limit)도 필요 없다.
     */
    private fun JPAQuery<Brand>.paginate(pagination: Pagination): JPAQuery<Brand> = this
        .orderBy(brand.name.desc())
        .offset(pagination.offset.toLong())
        .limit(pagination.size.toLong())
}
