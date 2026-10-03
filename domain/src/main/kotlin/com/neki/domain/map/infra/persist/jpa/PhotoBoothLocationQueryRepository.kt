package com.neki.domain.map.infra.persist.jpa

import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.PhotoBoothLocationWithDistance
import com.neki.domain.map.models.QBrand.brand
import com.neki.domain.map.models.QPhotoBoothLocation.photoBoothLocation
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.StringExpression
import com.querydsl.core.types.dsl.StringPath
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Point
import org.locationtech.jts.io.WKTReader
import org.springframework.stereotype.Repository
import kotlin.math.cos

/**
 * fileName       : PhotoBoothLocationQueryRepository
 * author         : darren
 * date           : 2026. 1. 17.
 * description    : PhotoBoothLocation QueryDSL Repository for pagination
 */
@Repository
class PhotoBoothLocationQueryRepository(
    private val queryFactory: JPAQueryFactory,
    private val entityManager: EntityManager,
) {

    /**
     * 다각형 내부의 포토부스 조회
     * @param coordinates 다각형을 구성하는 좌표 리스트 (경도, 위도)
     * @param brandIds 브랜드 ID 리스트 (nullable)
     */
    fun findByPolygon(coordinates: List<Coordinate>, brandIds: List<Long>?): List<PhotoBoothLocationView> {
        // LINESTRING 생성을 위한 좌표 문자열 생성
        val lineString: String = coordinates.joinToString(", ") { "${it.x} ${it.y}" }

        val query = queryFactory
            .select(
                Projections.constructor(
                    PhotoBoothLocationView::class.java,
                    photoBoothLocation.id,
                    brand.name,
                    photoBoothLocation.branchName,
                    photoBoothLocation.address,
                    photoBoothLocation.location,
                ),
            )
            .from(photoBoothLocation)
            .leftJoin(brand).on(brand.id.eq(photoBoothLocation.brandId))
            .where(
                Expressions.booleanTemplate(
                    "ST_Contains(ST_MakePolygon(ST_GeomFromText('LINESTRING($lineString)', 4326)), {0}) = true",
                    photoBoothLocation.location,
                ),
                brandIds?.takeIf { it.isNotEmpty() }?.let { photoBoothLocation.brandId.`in`(it) },
            )

        return query.fetch()
    }

    /**
     * 지점명, 브랜드명, `브랜드명 지점명` 중 하나가 keyword 로 시작하는 포토부스.
     * 위치가 있으면 가까운 순, 없으면 브랜드명, 지점명 순이고 같은 값이면 id 순이다.
     */
    fun findByKeyword(query: MapQuery.SearchPhotoBooths): List<PhotoBoothLocationView> = queryFactory
        .select(
            Projections.constructor(
                PhotoBoothLocationView::class.java,
                photoBoothLocation.id,
                brand.name,
                photoBoothLocation.branchName,
                photoBoothLocation.address,
                photoBoothLocation.location,
            ),
        )
        .from(photoBoothLocation)
        .join(brand).on(brand.id.eq(photoBoothLocation.brandId), brand.deletedAt.isNull)
        .where(matches(query))
        .orderBy(relevance(query).asc(), *nameSearchOrder(query.coordinate))
        .offset(query.pagination.offset.toLong())
        .limit(query.pagination.limit.toLong())
        .fetch()

    fun countByKeyword(query: MapQuery.SearchPhotoBooths): Long = queryFactory
        .select(photoBoothLocation.count())
        .from(photoBoothLocation)
        .join(brand).on(brand.id.eq(photoBoothLocation.brandId), brand.deletedAt.isNull)
        .where(matches(query))
        .fetchOne() ?: 0L

    /**
     * 지점명이 있고, 검색어 전체가 이름 앞부분이거나 낱말마다 브랜드명·지점명·주소 중 하나에 들어 있는 부스.
     * `강남 포토그레이` 는 브랜드명에 `포토그레이`, 지점명이나 주소에 `강남` 이 있는 부스다. 낱말 순서는 상관없다.
     * 중간 일치라 인덱스를 못 타지만 이 쿼리는 원래 전체를 훑는다.
     */
    private fun matches(query: MapQuery.SearchPhotoBooths): BooleanExpression {
        val nameStartsWith: BooleanExpression = nameStartsWith(query.keyword)
        val termsMatched: BooleanExpression = everyTermIn(query.terms, withAddress = true) ?: return nameStartsWith
        return hasOwnBranchName().and(nameStartsWith.or(termsMatched))
    }

    /**
     * 0 : 검색어 전체가 이름 앞부분 (낱말 검색을 넣기 전 결과. 이 순서 그대로 맨 위에 온다)
     * 1 : 낱말이 전부 브랜드명·지점명에 있음 (`서울강남점`)
     * 2 : 주소까지 봐야 맞음 (강남구의 `대치동점`)
     */
    private fun relevance(query: MapQuery.SearchPhotoBooths): NumberExpression<Int> {
        val startsWith = CaseBuilder().`when`(nameStartsWith(query.keyword)).then(0)
        val inName: BooleanExpression = everyTermIn(query.terms, withAddress = false) ?: return startsWith.otherwise(2)
        return startsWith.`when`(inName).then(1).otherwise(2)
    }

    /** 낱말마다 같은 뜻의 이름 중 하나가 브랜드명·지점명(·주소)에 들어 있는가. 낱말이 없으면 null */
    private fun everyTermIn(terms: List<List<String>>, withAddress: Boolean): BooleanExpression? {
        if (terms.isEmpty()) return null

        val perTerm: List<BooleanExpression> = terms.map { names ->
            val matched: List<BooleanExpression> = names.flatMap { name ->
                listOfNotNull(
                    brand.name.containsIgnoreCase(name),
                    photoBoothLocation.branchName.containsIgnoreCase(name),
                    photoBoothLocation.address.containsIgnoreCase(name).takeIf { withAddress },
                )
            }
            Expressions.anyOf(*matched.toTypedArray())
        }
        return Expressions.allOf(*perTerm.toTypedArray())
    }

    /**
     * 검색어 전체가 이름 앞부분인가. `강남` 은 지점명, `포토이즘` 은 브랜드명, `포토이즘 강남` 은 둘을 이은 것.
     * branch_name 에 인덱스가 없어 어차피 전체를 훑으므로 대소문자를 무시한다 (`서울NC송파점` 을 `서울nc` 로).
     */
    private fun nameStartsWith(keyword: String): BooleanExpression =
        photoBoothLocation.branchName.startsWithIgnoreCase(keyword)
            .or(brand.name.startsWithIgnoreCase(keyword))
            .or(brand.name.concat(" ").concat(photoBoothLocation.branchName).startsWithIgnoreCase(keyword))

    /**
     * 지점명이 비었거나 브랜드명과 같은 부스는 어느 지점인지 알 수 없어 검색에서 뺀다.
     * 브랜드명과의 비교는 검색 색인(SearchNormalizer.normalize)처럼 대소문자와 공백·`-`·`_` 를 무시한다.
     */
    private fun hasOwnBranchName(): BooleanExpression {
        val branchName: StringExpression = normalized(photoBoothLocation.branchName)
        return branchName.ne("").and(branchName.ne(normalized(brand.name)))
    }

    private fun normalized(path: StringPath): StringExpression =
        Expressions.stringTemplate("lower(replace(replace(replace({0}, ' ', ''), '-', ''), '_', ''))", path)

    private fun nameSearchOrder(coordinate: Coordinate?): Array<OrderSpecifier<*>> {
        val byId: OrderSpecifier<Long> = photoBoothLocation.id.asc()
        if (coordinate == null) {
            return arrayOf(codePointOrder(brand.name), codePointOrder(photoBoothLocation.branchName), byId)
        }

        // 경도 1도의 길이는 위도에 따라 줄어든다. cos(위도)를 곱해 위도 1도와 같은 척도로 맞춘다
        val longitudeScale: Double = cos(Math.toRadians(coordinate.y))
        val distance: NumberExpression<Double> = Expressions.numberTemplate(
            Double::class.java,
            "approx_distance_order({0}, {1}, {2}, {3})",
            photoBoothLocation.location,
            coordinate.x,
            coordinate.y,
            longitudeScale,
        )
        return arrayOf(distance.asc(), byId)
    }

    /** DB 기본 collation 대신 코드포인트 순. modules/postgres 의 CodePointCollationFunctionContributor 참조 */
    private fun codePointOrder(path: StringPath): OrderSpecifier<String> =
        Expressions.stringTemplate("code_point_collate({0})", path).asc()

    /**
     * geography문법이 Hibernate/QueryDSL에서 파싱 불가 따라서 Native Query 작성
     * 특정 좌표 기준 거리순 포토부스 조회
     * @param longitude 경도
     * @param latitude 위도
     * @param radiusInMeters 검색 반경 (미터)
     * @param brandIds 브랜드 ID 리스트 (nullable)
     */
    fun findByDistanceFromPoint(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
    ): List<PhotoBoothLocationWithDistance> {
        val sql = """
            SELECT
                TB_PHOTO_BOOTH_LOCATION.id,
                TB_BRAND.name,
                TB_PHOTO_BOOTH_LOCATION.branch_name,
                TB_PHOTO_BOOTH_LOCATION.address,
                ST_AsText(TB_PHOTO_BOOTH_LOCATION.location) as location_wkt,
                CAST(ST_Distance(
                    location::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                ) AS integer) AS distance_meters
            FROM TB_PHOTO_BOOTH_LOCATION
            LEFT JOIN TB_BRAND on TB_BRAND.id = TB_PHOTO_BOOTH_LOCATION.brand_id
            WHERE ST_DWithin(
                location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                :radiusInMeters
            )
            ${if (!brandIds.isNullOrEmpty()) "AND brand_id IN (:brandIds)" else ""}
            ORDER BY distance_meters
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql)
            .setParameter("longitude", coordinate.x)
            .setParameter("latitude", coordinate.y)
            .setParameter("radiusInMeters", radiusInMeters)

        if (!brandIds.isNullOrEmpty()) {
            query.setParameter("brandIds", brandIds)
        }

        @Suppress("UNCHECKED_CAST")
        val results = query.resultList as List<Array<Any>>

        val wktReader = WKTReader()

        return results.map { row ->
            // WKT 문자열을 JTS Point로 파싱
            val locationWkt = row[4] as String
            val jtsPoint = wktReader.read(locationWkt) as Point

            PhotoBoothLocationWithDistance(
                id = (row[0] as Number).toLong(),
                brandName = row[1] as String,
                branchName = row[2] as String,
                address = row[3] as String,
                location = jtsPoint,
                distance = (row[5] as Number).toInt(),
            )
        }
    }
}
