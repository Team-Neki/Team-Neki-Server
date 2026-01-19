package com.yapp2app.map.infra.persist.jpa

import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.map.domain.entity.QPhotoBoothLocation.photoBoothLocation
import jakarta.persistence.EntityManager
import org.locationtech.jts.geom.Point
import org.locationtech.jts.io.WKTReader
import org.springframework.stereotype.Repository

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
     * @param offset 페이지네이션 offset
     * @param limit 페이지네이션 limit
     */
    fun findByPolygon(
        coordinates: List<Pair<Double, Double>>,
        brandIds: List<Long>?,
        offset: Int,
        limit: Int,
    ): List<PhotoBoothLocationDto> {
        // LINESTRING 생성을 위한 좌표 문자열 생성
        val lineString = coordinates.joinToString(", ") { "${it.first} ${it.second}" }

        val query = queryFactory
            .select(
                Projections.constructor(
                    PhotoBoothLocationDto::class.java,
                    photoBoothLocation.id,
                    photoBoothLocation.brandId,
                    photoBoothLocation.name,
                    photoBoothLocation.address,
                    photoBoothLocation.location,
                ),
            )
            .from(photoBoothLocation)
            .where(
                Expressions.booleanTemplate(
                    "ST_Contains(ST_MakePolygon(ST_GeomFromText('LINESTRING($lineString)', 4326)), {0}) = true",
                    photoBoothLocation.location,
                ),
                brandIds?.takeIf { it.isNotEmpty() }?.let { photoBoothLocation.brandId.`in`(it) },
            )
            .offset(offset.toLong())
            .limit(limit.toLong())

        return query.fetch()
    }

    /**
     * geography문법이 Hibernate/QueryDSL에서 파싱 불가 따라서 Native Query 작성
     * 특정 좌표 기준 거리순 포토부스 조회
     * @param longitude 경도
     * @param latitude 위도
     * @param radiusInMeters 검색 반경 (미터)
     * @param brandIds 브랜드 ID 리스트 (nullable)
     * @param offset 페이지네이션 offset
     * @param limit 페이지네이션 limit
     */
    fun findByDistanceFromPoint(
        longitude: Double,
        latitude: Double,
        radiusInMeters: Int,
        brandIds: List<Long>?,
        offset: Int,
        limit: Int,
    ): List<PhotoBoothLocationWithDistanceDto> {
        val sql = """
            SELECT
                id, brand_id, name, address, ST_AsText(location) as location_wkt,
                CAST(ST_Distance(
                    location::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                ) AS integer) AS distance_meters
            FROM tb_photo_booth_location
            WHERE ST_DWithin(
                location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                :radiusInMeters
            )
            ${if (!brandIds.isNullOrEmpty()) "AND brand_id IN (:brandIds)" else ""}
            ORDER BY distance_meters
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql)
            .setParameter("longitude", longitude)
            .setParameter("latitude", latitude)
            .setParameter("radiusInMeters", radiusInMeters)
            .setParameter("limit", limit)
            .setParameter("offset", offset)

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

            PhotoBoothLocationWithDistanceDto(
                id = (row[0] as Number).toLong(),
                brandId = (row[1] as Number).toLong(),
                name = row[2] as String,
                address = row[3] as String,
                location = jtsPoint,
                distance = (row[5] as Number).toInt(),
            )
        }
    }
}

/**
 * 포토부스 위치 DTO
 */
data class PhotoBoothLocationDto(
    val id: Long,
    val brandId: Long,
    val name: String,
    val address: String,
    val location: Point,
)

/**
 * 거리 정보를 포함한 포토부스 위치 DTO
 */
data class PhotoBoothLocationWithDistanceDto(
    val id: Long,
    val brandId: Long,
    val name: String,
    val address: String,
    val location: Point,
    val distance: Int,
)
