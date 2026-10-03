package com.neki.domain.search.infra.persist.jpa

import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.QNearbyStation.nearbyStation
import com.neki.domain.search.models.QPhotoBoothSearch.photoBoothSearch
import com.neki.domain.search.models.SearchTarget
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoBoothSearchQueryRepository
 * author         : koo
 * date           : 2026. 9. 30.
 * description    : 검색 색인(_read) 조회. 지역은 region_ids GIN, 역은 _station 의 (station_name, line_name) 인덱스를 탄다
 */
@Repository
class PhotoBoothSearchQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findByArea(area: SearchTarget.Area, brandIds: List<Long>?): List<PhotoBoothSearch> {
        val brandIn: BooleanExpression? =
            brandIds?.takeIf { it.isNotEmpty() }?.let { photoBoothSearch.brandId.`in`(it) }

        return when (area) {
            is SearchTarget.Region ->
                queryFactory
                    .selectFrom(photoBoothSearch)
                    .where(
                        // PostgreSQL 에서 `region_ids @> array[?]` 로 렌더링되어 GIN 인덱스를 탄다. H2 에서는 array_contains 그대로
                        Expressions.booleanTemplate("array_contains({0}, {1})", photoBoothSearch.regionIds, area.code),
                        brandIn,
                    )
                    .fetch()

            is SearchTarget.Station ->
                queryFactory
                    .selectFrom(photoBoothSearch)
                    .join(photoBoothSearch.stations, nearbyStation)
                    .where(
                        nearbyStation.stationName.eq(area.name),
                        nearbyStation.lineName.eq(area.lineName),
                        brandIn,
                    )
                    .fetch()
        }
    }

    fun findBrandNames(): Map<Long, String> = queryFactory
        .select(photoBoothSearch.brandId, photoBoothSearch.brandName)
        .distinct()
        .from(photoBoothSearch)
        .fetch()
        .associate { it.get(photoBoothSearch.brandId)!! to it.get(photoBoothSearch.brandName)!! }
}
