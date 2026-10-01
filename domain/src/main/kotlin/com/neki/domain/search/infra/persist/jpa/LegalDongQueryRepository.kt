package com.neki.domain.search.infra.persist.jpa

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.QLegalDong.legalDong
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : LegalDongQueryRepository
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 법정동 접두 검색. startsWith 는 `LIKE 'prefix%'` 로 나가 leaf_name, full_name 의 text_pattern_ops 인덱스를 탄다.
 *                  하위 구역 조건은 full_name 접두 여러 개의 OR 이라 PostgreSQL 이 BitmapOr 로 묶는다.
 */
@Repository
class LegalDongQueryRepository(private val queryFactory: JPAQueryFactory) {

    /** 이름이 prefix 로 시작하는 시군구 이하 법정동의 전체 경로. 하위 구역을 찾는 기준이 된다 */
    fun findFullNamesByNamePrefix(prefix: String): List<String> = queryFactory
        .select(legalDong.fullName)
        .from(legalDong)
        .where(legalDong.leafName.startsWith(prefix), legalDong.level.gt(LegalDong.SIDO_LEVEL))
        .fetch()

    fun findSelfOrDescendants(fullNames: List<String>, pathPrefix: String?, pagination: Pagination): List<LegalDong> {
        val condition: BooleanExpression = selfOrDescendantOf(fullNames, pathPrefix) ?: return emptyList()
        return queryFactory
            .selectFrom(legalDong)
            .where(condition)
            .orderBy(legalDong.level.asc(), legalDong.code.asc())
            .offset(pagination.offset.toLong())
            .limit(pagination.limit.toLong())
            .fetch()
    }

    fun countSelfOrDescendants(fullNames: List<String>, pathPrefix: String?): Long {
        val condition: BooleanExpression = selfOrDescendantOf(fullNames, pathPrefix) ?: return 0L
        return queryFactory
            .select(legalDong.count())
            .from(legalDong)
            .where(condition)
            .fetchOne() ?: 0L
    }

    /**
     * fullNames 의 구역 자신과 그 아래 구역, 그리고 전체 경로가 pathPrefix 로 시작하는 구역.
     * 하위 구역은 전체 경로가 `상위 경로 + 공백` 으로 시작한다. 코드 앞자리로는 묶이지 않는다
     * (수원시 4111000000 아래 장안구가 4111100000). 둘 다 없으면 null.
     */
    private fun selfOrDescendantOf(fullNames: List<String>, pathPrefix: String?): BooleanExpression? {
        val conditions: List<BooleanExpression> = buildList {
            if (fullNames.isNotEmpty()) add(legalDong.fullName.`in`(fullNames))
            fullNames.forEach { add(legalDong.fullName.startsWith("$it ")) }
            pathPrefix?.let { add(legalDong.fullName.startsWith(it)) }
        }
        if (conditions.isEmpty()) return null

        return Expressions.anyOf(*conditions.toTypedArray()).and(legalDong.level.gt(LegalDong.SIDO_LEVEL))
    }
}
