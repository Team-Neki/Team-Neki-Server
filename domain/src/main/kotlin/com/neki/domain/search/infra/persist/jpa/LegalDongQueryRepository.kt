package com.neki.domain.search.infra.persist.jpa

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.QLegalDong.legalDong
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : LegalDongQueryRepository
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 법정동 접두 검색. startsWith 는 `LIKE 'prefix%'` 로 나가 leaf_name, full_name 의 text_pattern_ops 인덱스를 탄다.
 *                  두 조건은 OR 이라 PostgreSQL 이 두 인덱스를 BitmapOr 로 함께 쓴다.
 */
@Repository
class LegalDongQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findByNameOrPathPrefix(namePrefix: String, pathPrefix: String?, pagination: Pagination): List<LegalDong> =
        queryFactory
            .selectFrom(legalDong)
            .where(nameOrPathStartsWith(namePrefix, pathPrefix))
            .orderBy(legalDong.level.asc(), legalDong.code.asc())
            .offset(pagination.offset.toLong())
            .limit(pagination.limit.toLong())
            .fetch()

    fun countByNameOrPathPrefix(namePrefix: String, pathPrefix: String?): Long = queryFactory
        .select(legalDong.count())
        .from(legalDong)
        .where(nameOrPathStartsWith(namePrefix, pathPrefix))
        .fetchOne() ?: 0L

    /**
     * `강남` 처럼 이 구역 이름으로 찾거나, `서울특별시 강남` 처럼 상위 경로부터 적어 찾는다.
     * 전체 경로로 찾으면 그 아래 구역도 함께 걸린다 (`서울특별시 강남구` 에 역삼동도 나온다).
     */
    private fun nameOrPathStartsWith(namePrefix: String, pathPrefix: String?): BooleanExpression {
        val nameStartsWith: BooleanExpression = legalDong.leafName.startsWith(namePrefix)
        val matched: BooleanExpression =
            pathPrefix?.let { nameStartsWith.or(legalDong.fullName.startsWith(it)) } ?: nameStartsWith

        return matched.and(legalDong.level.gt(LegalDong.SIDO_LEVEL))
    }

    fun findByFullName(fullName: String): LegalDong? = queryFactory
        .selectFrom(legalDong)
        .where(legalDong.fullName.eq(fullName), legalDong.level.gt(LegalDong.SIDO_LEVEL))
        .orderBy(legalDong.code.asc())
        .fetchFirst()

    fun findSeoulDistricts(): List<LegalDong> = queryFactory
        .selectFrom(legalDong)
        .where(legalDong.level.eq(LegalDong.SIGUNGU_LEVEL), legalDong.code.startsWith(LegalDong.SEOUL_CODE_PREFIX))
        .fetch()
}
