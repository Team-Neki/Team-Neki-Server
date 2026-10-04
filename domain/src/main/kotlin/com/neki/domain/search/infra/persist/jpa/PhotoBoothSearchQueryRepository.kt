package com.neki.domain.search.infra.persist.jpa

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.QNearbyStation.nearbyStation
import com.neki.domain.search.models.QPhotoBoothSearch.photoBoothSearch
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.UserLocation
import com.neki.domain.search.service.qu.SearchNormalizer
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.StringPath
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import kotlin.math.cos

/**
 * fileName       : PhotoBoothSearchQueryRepository
 * author         : koo
 * date           : 2026. 9. 30.
 * description    : 검색 색인(_read) 조회. 지역은 region_ids GIN, 역은 _station 의 (station_name, line_name) 인덱스,
 *                  지점은 (platform, idx) 유니크 인덱스를 탄다. 부스 자동완성(keyword)은 정규화 이름 컬럼을 훑는다
 */
@Repository
class PhotoBoothSearchQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findByScope(scope: SearchTarget.Scope, brandIds: List<Long>?): List<PhotoBoothSearch> {
        val brandIn: BooleanExpression? =
            brandIds?.takeIf { it.isNotEmpty() }?.let { photoBoothSearch.brandId.`in`(it) }

        return when (scope) {
            is SearchTarget.Region ->
                queryFactory
                    .selectFrom(photoBoothSearch)
                    .where(
                        // PostgreSQL 에서 `region_ids @> array[?]` 로 렌더링되어 GIN 인덱스를 탄다. H2 에서는 array_contains 그대로
                        Expressions.booleanTemplate("array_contains({0}, {1})", photoBoothSearch.regionIds, scope.code),
                        brandIn,
                    )
                    .fetch()

            is SearchTarget.Station ->
                queryFactory
                    .selectFrom(photoBoothSearch)
                    .join(photoBoothSearch.stations, nearbyStation)
                    .where(
                        nearbyStation.stationName.eq(scope.name),
                        nearbyStation.lineName.eq(scope.lineName),
                        brandIn,
                    )
                    .fetch()

            is SearchTarget.Booth ->
                queryFactory
                    .selectFrom(photoBoothSearch)
                    .where(photoBoothSearch.platform.eq(scope.platform), photoBoothSearch.idx.eq(scope.idx), brandIn)
                    .fetch()
        }
    }

    /**
     * 부스 자동완성. 검색어 전체가 이름 앞부분이거나, 낱말(terms)마다 브랜드명·지점명·주소 중 하나에 들어 있는 행.
     * 이름은 색인의 정규화 컬럼과 비교하므로 대소문자와 공백·`-`·`_` 를 무시한다 (`플랜비스` 도 `플랜비 스튜디오` 로 찾는다).
     * 이름 앞부분 일치 → 낱말이 전부 이름에 있음 → 주소로 맞음 순이고, 그 안에서 userLocation 이 있으면 가까운 순,
     * 없으면 브랜드명, 지점명 순이다. 중간 일치라 인덱스를 못 타고 전체를 훑는다 (지금 1,653 행).
     */
    fun findByKeyword(
        keyword: String,
        terms: List<List<String>>,
        userLocation: UserLocation?,
        pagination: Pagination,
    ): List<PhotoBoothSearch> = queryFactory
        .selectFrom(photoBoothSearch)
        .where(keywordMatches(keyword, terms))
        .orderBy(relevance(keyword, terms).asc(), *keywordOrder(userLocation))
        .offset(pagination.offset.toLong())
        .limit(pagination.limit.toLong())
        .fetch()

    fun countByKeyword(keyword: String, terms: List<List<String>>): Long = queryFactory
        .select(photoBoothSearch.count())
        .from(photoBoothSearch)
        .where(keywordMatches(keyword, terms))
        .fetchOne() ?: 0L

    /** 지점명이 있고, 검색어 전체가 이름 앞부분이거나 낱말이 전부 브랜드명·지점명·주소에 있는 행 */
    private fun keywordMatches(keyword: String, terms: List<List<String>>): BooleanExpression {
        val matched: BooleanExpression = everyTermIn(terms, withAddress = true)
            ?.let { nameStartsWith(keyword).or(it) }
            ?: nameStartsWith(keyword)
        return hasOwnBranchName().and(matched)
    }

    /**
     * 0 : 검색어 전체가 이름 앞부분
     * 1 : 낱말이 전부 브랜드명·지점명에 있음 (`서울강남점`)
     * 2 : 주소까지 봐야 맞음 (강남구의 `대치동점`)
     */
    private fun relevance(keyword: String, terms: List<List<String>>): NumberExpression<Int> {
        val startsWith = CaseBuilder().`when`(nameStartsWith(keyword)).then(0)
        val inName: BooleanExpression = everyTermIn(terms, withAddress = false) ?: return startsWith.otherwise(2)
        return startsWith.`when`(inName).then(1).otherwise(2)
    }

    /** 지점명, 브랜드명, `브랜드명 지점명` 중 하나가 검색어로 시작하는가. 셋 다 정규화 값끼리 비교한다 */
    private fun nameStartsWith(keyword: String): BooleanExpression {
        val prefix: String = SearchNormalizer.normalize(keyword)
        return photoBoothSearch.normalizedBranchName.startsWith(prefix)
            .or(photoBoothSearch.normalizedBrandName.startsWith(prefix))
            .or(photoBoothSearch.normalizedBrandName.concat(photoBoothSearch.normalizedBranchName).startsWith(prefix))
    }

    /**
     * 낱말마다 같은 뜻의 이름 중 하나가 브랜드명·지점명(·주소)에 들어 있는가. 낱말이 없으면 null.
     * 주소는 정규화 컬럼이 없어 원문과 대소문자만 무시하고 비교한다.
     */
    private fun everyTermIn(terms: List<List<String>>, withAddress: Boolean): BooleanExpression? {
        if (terms.isEmpty()) return null

        val perTerm: List<BooleanExpression> = terms.map { names ->
            val matched: List<BooleanExpression> = names.flatMap { name ->
                val normalized: String = SearchNormalizer.normalize(name)
                listOfNotNull(
                    photoBoothSearch.normalizedBrandName.contains(normalized).takeIf { normalized.isNotEmpty() },
                    photoBoothSearch.normalizedBranchName.contains(normalized).takeIf { normalized.isNotEmpty() },
                    photoBoothSearch.address.containsIgnoreCase(name).takeIf { withAddress },
                )
            }
            Expressions.anyOf(*matched.toTypedArray()) ?: Expressions.FALSE.isTrue
        }
        return Expressions.allOf(*perTerm.toTypedArray())
    }

    /** 지점명이 비었거나 브랜드명과 같은 행은 어느 지점인지 알 수 없어 자동완성에서 뺀다 */
    private fun hasOwnBranchName(): BooleanExpression = photoBoothSearch.normalizedBranchName.ne("")
        .and(photoBoothSearch.normalizedBranchName.ne(photoBoothSearch.normalizedBrandName))

    /**
     * userLocation 이 있으면 가까운 순, 없으면 브랜드명, 지점명 순. 같으면 원천 키 순이다.
     * 색인 id 는 세대마다 바뀌므로 동률을 원천 키로 가른다.
     * `approx_distance_order`, `code_point_collate` 는 modules/postgres 가 등록한다 (SubwayStationQueryRepository 참조).
     */
    private fun keywordOrder(userLocation: UserLocation?): Array<OrderSpecifier<*>> {
        val bySource: Array<OrderSpecifier<String>> =
            arrayOf(photoBoothSearch.platform.asc(), photoBoothSearch.idx.asc())
        if (userLocation == null) {
            return arrayOf(
                codePointOrder(photoBoothSearch.brandName),
                codePointOrder(photoBoothSearch.branchName),
                *bySource,
            )
        }

        val distance: NumberExpression<Double> = Expressions.numberTemplate(
            Double::class.java,
            "approx_distance_order({0}, {1}, {2}, {3})",
            photoBoothSearch.location,
            userLocation.longitude,
            userLocation.latitude,
            cos(Math.toRadians(userLocation.latitude)),
        )
        return arrayOf(distance.asc(), *bySource)
    }

    private fun codePointOrder(path: StringPath): OrderSpecifier<String> =
        Expressions.stringTemplate("code_point_collate({0})", path).asc()

    // ponytail: 이어 붙인 이름 비교라 인덱스 없이 전체를 읽는다 (지금 1,653 행). 수만 행이 되면 (brand_name, branch_name) 인덱스를 걸고 공백 위치마다 나눠 비교한다
    fun findByBoothName(boothName: String): List<PhotoBoothSearch> = queryFactory
        .selectFrom(photoBoothSearch)
        .where(photoBoothSearch.brandName.concat(" ").concat(photoBoothSearch.branchName).eq(boothName))
        .fetch()

    fun findBrandNames(): Map<Long, String> = queryFactory
        .select(photoBoothSearch.brandId, photoBoothSearch.brandName)
        .distinct()
        .from(photoBoothSearch)
        .fetch()
        .associate { it.get(photoBoothSearch.brandId)!! to it.get(photoBoothSearch.brandName)!! }
}
