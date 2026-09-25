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
 * description    : 법정동 접두 검색. startsWith 는 `LIKE 'prefix%'` 로 나가 leaf_name 의 text_pattern_ops 인덱스를 탄다.
 */
@Repository
class LegalDongQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findByLeafNamePrefix(prefix: String, pagination: Pagination): List<LegalDong> = queryFactory
        .selectFrom(legalDong)
        .where(leafNameStartsWith(prefix))
        .orderBy(legalDong.level.asc(), legalDong.code.asc())
        .offset(pagination.offset.toLong())
        .limit(pagination.limit.toLong())
        .fetch()

    fun countByLeafNamePrefix(prefix: String): Long = queryFactory
        .select(legalDong.count())
        .from(legalDong)
        .where(leafNameStartsWith(prefix))
        .fetchOne() ?: 0L

    private fun leafNameStartsWith(prefix: String): BooleanExpression =
        legalDong.leafName.startsWith(prefix).and(legalDong.level.gt(LegalDong.SIDO_LEVEL))
}
