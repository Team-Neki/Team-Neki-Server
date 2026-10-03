package com.neki.domain.search.infra.persist.jpa

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.QSubwayStation.subwayStation
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.UserLocation
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.StringPath
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import kotlin.math.cos

/**
 * fileName       : SubwayStationQueryRepository
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 지하철역 접두 검색. startsWith 는 `LIKE 'prefix%'` 로 나가 name 의 text_pattern_ops 인덱스를 탄다.
 *                  `강남역 2호선` 처럼 노선명까지 적으면 역명 접두로 인덱스에서 후보를 좁힌 뒤 이어 붙인 이름으로 거른다.
 *                  위치가 있으면 가까운 순으로 정렬한다.
 */
@Repository
class SubwayStationQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findByKeywordPrefix(
        keyword: String,
        namePrefix: String,
        userLocation: UserLocation?,
        pagination: Pagination,
    ): List<SubwayStation> = queryFactory
        .selectFrom(subwayStation)
        .where(keywordStartsWith(keyword, namePrefix))
        .orderBy(*order(userLocation))
        .offset(pagination.offset.toLong())
        .limit(pagination.limit.toLong())
        .fetch()

    fun countByKeywordPrefix(keyword: String, namePrefix: String): Long = queryFactory
        .select(subwayStation.count())
        .from(subwayStation)
        .where(keywordStartsWith(keyword, namePrefix))
        .fetchOne() ?: 0L

    /**
     * 공백이 없으면 역명 접두만 본다. 공백이 있으면 노선명까지 적은 것으로 보고 `강남역 2호선`, `강남 2호선` 두 모양을 모두 받는다.
     * 노선명(`부산 도시철도 2호선`)과 역명(`북구청 (대구iM뱅크파크)`)에도 공백이 있어 검색어를 쪼개지 않고 이어 붙인 이름과 비교한다.
     */
    private fun keywordStartsWith(keyword: String, namePrefix: String): BooleanExpression {
        val name: StringPath = subwayStation.id.name
        val lineName: StringPath = subwayStation.id.lineName
        val nameStartsWith: BooleanExpression = name.startsWith(namePrefix)
        if (!keyword.contains(' ')) return nameStartsWith

        val withSuffix: BooleanExpression = name.concat("$STATION_SUFFIX ").concat(lineName).startsWith(keyword)
        val withoutSuffix: BooleanExpression = name.concat(" ").concat(lineName).startsWith(keyword)
        return nameStartsWith.and(withSuffix.or(withoutSuffix))
    }

    /**
     * userLocation 이 있으면 가까운 순, 같거나 없으면 역명, 노선명 순.
     * 가까운 순은 modules/postgres 의 ApproxDistanceFunctionContributor 가 등록한 `approx_distance_order` 로,
     * 경도 차이에 cos(위도)를 곱해 위도와 같은 척도로 맞춘 평면 거리의 제곱이다. 정렬에만 쓴다.
     */
    private fun order(userLocation: UserLocation?): Array<OrderSpecifier<*>> {
        val byName: Array<OrderSpecifier<String>> =
            arrayOf(codePointOrder(subwayStation.id.name), codePointOrder(subwayStation.id.lineName))
        if (userLocation == null) return arrayOf(*byName)

        val distance: NumberExpression<Double> = Expressions.numberTemplate(
            Double::class.java,
            "approx_distance_order({0}, {1}, {2}, {3})",
            subwayStation.location,
            userLocation.longitude,
            userLocation.latitude,
            cos(Math.toRadians(userLocation.latitude)),
        )
        return arrayOf(distance.asc(), *byName)
    }

    /**
     * DB 기본 collation(en_US.utf8)은 `강남대` 를 `강남구청` 앞에, `신분당선` 을 `2호선` 앞에 둔다.
     * 테이블은 워크플로가 만들어 컬럼 collation 을 바꿀 수 없으므로 정렬할 때 코드포인트 순을 지정한다.
     * `code_point_collate` 는 modules/postgres 의 CodePointCollationFunctionContributor 가 등록한다.
     */
    private fun codePointOrder(path: StringPath): OrderSpecifier<String> =
        Expressions.stringTemplate("code_point_collate({0})", path).asc()

    companion object {
        private const val STATION_SUFFIX = "역"
    }
}
