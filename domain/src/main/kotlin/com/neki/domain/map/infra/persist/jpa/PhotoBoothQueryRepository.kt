package com.neki.domain.map.infra.persist.jpa

import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.models.PhotoBoothWithDistance
import com.neki.domain.map.models.QBrand.brand
import com.neki.domain.map.models.QEnrichedPhotoBooth.enrichedPhotoBooth
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.Tuple
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Point
import org.locationtech.jts.io.WKTReader
import org.springframework.stereotype.Repository
import kotlin.math.cos

/**
 * fileName       : PhotoBoothQueryRepository
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스(tb_photo_booth_enriched) QueryDSL Repository.
 *   브랜드는 platform 과 같은 code 의 살아있는 브랜드로 붙이고(V38), 브랜드가 없거나 좌표가 없는 부스는 지도에 내리지 않는다
 */
@Repository
class PhotoBoothQueryRepository(private val queryFactory: JPAQueryFactory, private val entityManager: EntityManager) {

    /** 지도에 나올 수 있는 부스인지. 목록 조회와 같이 좌표와 살아있는 브랜드가 있어야 한다 */
    fun existsByKey(key: PhotoBoothKey): Boolean = queryFactory
        .selectOne()
        .from(enrichedPhotoBooth)
        .join(
            brand,
        ).on(brand.code.eq(enrichedPhotoBooth.key.platform), brand.deletedAt.isNull, brand.exposeToMap.isTrue)
        .where(enrichedPhotoBooth.key.eq(key), enrichedPhotoBooth.location.isNotNull)
        .fetchFirst() != null

    fun findExistingKeys(keys: Collection<PhotoBoothKey>): Set<PhotoBoothKey> {
        if (keys.isEmpty()) return emptySet()

        return queryFactory
            .select(enrichedPhotoBooth.key)
            .from(enrichedPhotoBooth)
            .join(brand).on(brand.code.eq(enrichedPhotoBooth.key.platform), brand.exposeToMap.isTrue)
            .where(anyKey(keys))
            .fetch()
            .toSet()
    }

    fun findByKeys(keys: Collection<PhotoBoothKey>): List<PhotoBoothView> {
        if (keys.isEmpty()) return emptyList()

        return selectPhotoBoothViews()
            .where(anyKey(keys))
            .fetch()
            .map(::toPhotoBoothView)
    }

    /**
     * 다각형 내부의 포토부스 조회
     * @param coordinates 다각형을 구성하는 좌표 리스트 (경도, 위도)
     * @param brandIds 브랜드 ID 리스트 (nullable)
     */
    fun findByPolygon(coordinates: List<Coordinate>, brandIds: List<Long>?): List<PhotoBoothView> {
        // LINESTRING 생성을 위한 좌표 문자열 생성
        val lineString: String = coordinates.joinToString(", ") { "${it.x} ${it.y}" }

        return selectPhotoBoothViews()
            .where(
                Expressions.booleanTemplate(
                    "ST_Contains(ST_MakePolygon(ST_GeomFromText('LINESTRING($lineString)', 4326)), {0}) = true",
                    enrichedPhotoBooth.location,
                ),
                brandIds?.takeIf { it.isNotEmpty() }?.let { brand.id.`in`(it) },
            )
            .fetch()
            .map(::toPhotoBoothView)
    }

    /**
     * geography 문법이 Hibernate/QueryDSL 에서 파싱 불가라 Native Query 로 작성 (V1 PhotoBoothLocationQueryRepository 와 같은 방식)
     * 특정 좌표 기준 거리순 포토부스 조회.
     * `location::geography` 조건은 geometry GIST 인덱스(Workflow 가 세대마다 건다, BACKEND-250)를 못 타서
     * 반경을 덮는 bbox(`&&`)로 먼저 거른 뒤 ST_DWithin 으로 정확히 자른다
     */
    fun findByDistanceFromPoint(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
    ): List<PhotoBoothWithDistance> {
        val sql = """
            SELECT
                e.platform,
                e.idx,
                b.name,
                e.name,
                e.address,
                ST_AsText(e.location) AS location_wkt,
                CAST(ST_Distance(
                    e.location::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                ) AS integer) AS distance_meters
            FROM tb_photo_booth_enriched e
            JOIN TB_BRAND b ON b.code = e.platform AND b.deleted_at IS NULL AND b.expose_to_map = true
            WHERE e.location IS NOT NULL
              AND e.location && ST_Expand(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), :radiusInDegrees)
              AND ST_DWithin(
                e.location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                :radiusInMeters
              )
            ${if (!brandIds.isNullOrEmpty()) "AND b.id IN (:brandIds)" else ""}
            ORDER BY distance_meters
        """.trimIndent()

        val query: Query = entityManager.createNativeQuery(sql)
            .setParameter("longitude", coordinate.x)
            .setParameter("latitude", coordinate.y)
            .setParameter("radiusInMeters", radiusInMeters)
            .setParameter("radiusInDegrees", radiusInDegrees(coordinate.y, radiusInMeters))

        if (!brandIds.isNullOrEmpty()) {
            query.setParameter("brandIds", brandIds)
        }

        @Suppress("UNCHECKED_CAST")
        val results = query.resultList as List<Array<Any?>>

        val wktReader = WKTReader()

        return results.map { row ->
            PhotoBoothWithDistance(
                key = PhotoBoothKey(platform = row[0] as String, idx = row[1] as String),
                brandName = row[2] as String,
                branchName = row[3] as String,
                address = row[4] as String?,
                location = wktReader.read(row[5] as String) as Point,
                distance = (row[6] as Number).toInt(),
            )
        }
    }

    /** 반경을 넉넉히 덮는 위경도 차. 경도 1도의 길이가 위도에 따라 줄어 cos 로 나누고, 구면·타원체 차이를 덮으려 10% 더한다 */
    private fun radiusInDegrees(latitude: Double, radiusInMeters: Int): Double =
        radiusInMeters / (METERS_PER_DEGREE * cos(Math.toRadians(latitude))) * BBOX_MARGIN

    private fun selectPhotoBoothViews(): JPAQuery<Tuple> = queryFactory
        .select(
            enrichedPhotoBooth.key.platform,
            enrichedPhotoBooth.key.idx,
            brand.name,
            enrichedPhotoBooth.name,
            enrichedPhotoBooth.address,
            enrichedPhotoBooth.location,
        )
        .from(enrichedPhotoBooth)
        .join(
            brand,
        ).on(brand.code.eq(enrichedPhotoBooth.key.platform), brand.deletedAt.isNull, brand.exposeToMap.isTrue)
        .where(enrichedPhotoBooth.location.isNotNull)

    private fun toPhotoBoothView(row: Tuple): PhotoBoothView = PhotoBoothView(
        key = PhotoBoothKey(
            platform = row.get(enrichedPhotoBooth.key.platform)!!,
            idx = row.get(enrichedPhotoBooth.key.idx)!!,
        ),
        brandName = row.get(brand.name)!!,
        branchName = row.get(enrichedPhotoBooth.name)!!,
        address = row.get(enrichedPhotoBooth.address),
        location = row.get(enrichedPhotoBooth.location)!!,
    )

    /**
     * 키 (platform, idx) 중 하나와 같은 부스. platform 은 브랜드 수만큼이라 platform 별 IN 을 OR 로 잇는다.
     * enriched 의 (platform, idx) 유니크 인덱스를 탄다.
     */
    private fun anyKey(keys: Collection<PhotoBoothKey>): BooleanBuilder {
        val condition = BooleanBuilder()
        keys.groupBy({ it.platform }, { it.idx }).forEach { (platform, idxs) ->
            condition.or(enrichedPhotoBooth.key.platform.eq(platform).and(enrichedPhotoBooth.key.idx.`in`(idxs)))
        }
        return condition
    }

    companion object {
        /** 위도 1도의 길이(m) */
        private const val METERS_PER_DEGREE: Double = 111_320.0
        private const val BBOX_MARGIN: Double = 1.1
    }
}
