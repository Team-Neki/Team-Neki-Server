package com.neki.domain.search.infra.persist.jpa

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.QLegalDong.legalDong
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.NumberExpression
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

    fun findByNamePrefix(prefix: String, pagination: Pagination): List<LegalDong> = queryFactory
        .selectFrom(legalDong)
        .where(nameStartsWith(prefix))
        .orderBy(legalDong.level.asc(), legalDong.code.asc())
        .offset(pagination.offset.toLong())
        .limit(pagination.limit.toLong())
        .fetch()

    fun countByNamePrefix(prefix: String): Long = queryFactory
        .select(legalDong.count())
        .from(legalDong)
        .where(nameStartsWith(prefix))
        .fetchOne() ?: 0L

    /**
     * `강남` 처럼 이 구역 이름으로 찾거나, `서울특별시 강남` 처럼 상위 경로부터 적어 찾는다.
     * 전체 경로로 찾을 때는 검색어가 상위 경로를 넘어 이 구역 이름까지 들어와야 한다.
     * 그래야 `서울특별시 강남구` 에 강남구만 나오고 그 아래 역삼동은 나오지 않는다 (이름 검색과 같은 규칙).
     */
    private fun nameStartsWith(prefix: String): BooleanExpression {
        val parentPathLength: NumberExpression<Int> = legalDong.fullName.length().subtract(legalDong.leafName.length())
        val fullNameStartsWith: BooleanExpression =
            legalDong.fullName.startsWith(prefix).and(parentPathLength.lt(prefix.length))

        return legalDong.leafName.startsWith(prefix).or(fullNameStartsWith)
            .and(legalDong.level.gt(LegalDong.SIDO_LEVEL))
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
