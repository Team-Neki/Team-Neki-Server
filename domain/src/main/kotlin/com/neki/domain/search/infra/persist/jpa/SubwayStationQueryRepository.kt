package com.neki.domain.search.infra.persist.jpa

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.QSubwayStation.subwayStation
import com.neki.domain.search.models.SubwayStation
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.StringPath
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : SubwayStationQueryRepository
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지하철역 접두 검색. startsWith 는 `LIKE 'prefix%'` 로 나가 name 의 text_pattern_ops 인덱스를 탄다.
 */
@Repository
class SubwayStationQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findByNamePrefix(prefix: String, pagination: Pagination): List<SubwayStation> = queryFactory
        .selectFrom(subwayStation)
        .where(nameStartsWith(prefix))
        .orderBy(codePointOrder(subwayStation.id.name), codePointOrder(subwayStation.id.lineName))
        .offset(pagination.offset.toLong())
        .limit(pagination.limit.toLong())
        .fetch()

    fun countByNamePrefix(prefix: String): Long = queryFactory
        .select(subwayStation.count())
        .from(subwayStation)
        .where(nameStartsWith(prefix))
        .fetchOne() ?: 0L

    private fun nameStartsWith(prefix: String): BooleanExpression = subwayStation.id.name.startsWith(prefix)

    /**
     * DB 기본 collation(en_US.utf8)은 `강남대` 를 `강남구청` 앞에, `신분당선` 을 `2호선` 앞에 둔다.
     * 테이블은 워크플로가 만들어 컬럼 collation 을 바꿀 수 없으므로 정렬할 때 코드포인트 순을 지정한다.
     * `code_point_collate` 는 modules/postgres 의 CodePointCollationFunctionContributor 가 등록한다.
     */
    private fun codePointOrder(path: StringPath): OrderSpecifier<String> =
        Expressions.stringTemplate("code_point_collate({0})", path).asc()
}
