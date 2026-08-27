package com.neki.api.map.application.usecase

import com.neki.api.map.application.GetPhotoBoothLocationUseCase
import com.neki.api.testfixture.FakeTransactionRunner
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.PhotoBoothLocationWithDistance
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.PhotoBoothLocationRepository
import com.neki.domain.map.service.MapService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

/**
 * fileName       : GetPhotoBoothLocationUseCaseTest
 * description    : GetPhotoBoothLocationUseCase 단위 테스트
 */
class GetPhotoBoothLocationUseCaseTest :
    FunSpec({

        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val userId = 1L

        lateinit var photoBoothLocationRepository: PhotoBoothLocationRepository
        lateinit var favoriteMapRepository: FavoriteMapRepository
        lateinit var useCase: GetPhotoBoothLocationUseCase

        beforeTest {
            photoBoothLocationRepository = mockk()
            favoriteMapRepository = mockk()
            // repository 는 mock, 도메인 서비스는 실제 구현을 사용해 UseCase -> Service -> Repository 경로를 검증한다
            useCase = GetPhotoBoothLocationUseCase(
                MapService(favoriteMapRepository, photoBoothLocationRepository),
                FakeTransactionRunner(),
            )
        }

        // ── Polygon 검색 ───────────────────────────────────────────────────────────

        test("Polygon 검색 - 결과가 있으면 위치 목록과 즐겨찾기 여부를 반환한다") {
            // Given
            val coordinates = listOf(
                Coordinate(127.0, 37.0),
                Coordinate(127.5, 37.0),
                Coordinate(127.5, 37.5),
                Coordinate(127.0, 37.5),
                Coordinate(127.0, 37.0),
            )
            val query = MapQuery.GetPolygonLocation(userId = userId, coordinates = coordinates, brandIds = null)

            val point1 = geometryFactory.createPoint(Coordinate(127.02, 37.49))
            val point2 = geometryFactory.createPoint(Coordinate(127.03, 37.50))
            val locationDtos = listOf(
                PhotoBoothLocationView(
                    id = 1L,
                    brandId = 1L,
                    brandName = "인생네컷",
                    branchName = "강남점",
                    address = "서울 강남구",
                    location = point1,
                ),
                PhotoBoothLocationView(
                    id = 2L,
                    brandId = 2L,
                    brandName = "하루필름",
                    branchName = "홍대점",
                    address = "서울 마포구",
                    location = point2,
                ),
            )

            every {
                photoBoothLocationRepository.listPolygonLocations(
                    coordinates = coordinates,
                    brandIds = null,
                )
            } returns locationDtos
            // 1번 부스만 즐겨찾기한 상태
            every { favoriteMapRepository.findFavoritedLocationIds(userId, listOf(1L, 2L)) } returns setOf(1L)

            // When
            val result = useCase.execute(query)

            // Then
            result.locations shouldHaveSize 2
            result.locations[0].id shouldBe 1L
            result.locations[1].id shouldBe 2L
            result.favoriteLocationIds shouldContainExactly setOf(1L)

            verify(exactly = 1) {
                photoBoothLocationRepository.listPolygonLocations(
                    coordinates = coordinates,
                    brandIds = null,
                )
            }
            verify(exactly = 1) { favoriteMapRepository.findFavoritedLocationIds(userId, listOf(1L, 2L)) }
        }

        test("Polygon 검색 - 결과가 없으면 빈 리스트를 반환하고 즐겨찾기를 조회하지 않는다") {
            // Given
            val coordinates = listOf(
                Coordinate(126.0, 36.0),
                Coordinate(126.5, 36.0),
                Coordinate(126.5, 36.5),
                Coordinate(126.0, 36.5),
                Coordinate(126.0, 36.0),
            )
            val query =
                MapQuery.GetPolygonLocation(userId = userId, coordinates = coordinates, brandIds = listOf(1L, 2L))

            every {
                photoBoothLocationRepository.listPolygonLocations(
                    coordinates = coordinates,
                    brandIds = listOf(1L, 2L),
                )
            } returns emptyList()

            // When
            val result = useCase.execute(query)

            // Then
            result.locations.shouldBeEmpty()
            result.favoriteLocationIds.shouldBeEmpty()
            verify(exactly = 0) { favoriteMapRepository.findFavoritedLocationIds(any(), any()) }
        }

        // ── Point 검색 ────────────────────────────────────────────────────────────

        test("Point 검색 - 결과가 있으면 거리 정보와 즐겨찾기 여부를 반환한다") {
            // Given
            val coordinate = Coordinate(127.0276, 37.4979)
            val query = MapQuery.GetPointLocation(
                userId = userId,
                coordinate = coordinate,
                radiusInMeters = 1000,
                brandIds = null,
            )

            val point = geometryFactory.createPoint(Coordinate(127.0280, 37.4985))
            val locationDtos = listOf(
                PhotoBoothLocationWithDistance(
                    id = 1L,
                    brandName = "인생네컷",
                    branchName = "강남점",
                    address = "서울 강남구 테헤란로 123",
                    location = point,
                    distance = 350,
                ),
            )

            every {
                photoBoothLocationRepository.listPointLocations(
                    coordinate = coordinate,
                    radiusInMeters = 1000,
                    brandIds = null,
                )
            } returns locationDtos
            every { favoriteMapRepository.findFavoritedLocationIds(userId, listOf(1L)) } returns setOf(1L)

            // When
            val result = useCase.execute(query)

            // Then
            result.locations shouldHaveSize 1
            result.locations[0].id shouldBe 1L
            result.locations[0].distance shouldBe 350
            result.favoriteLocationIds shouldContainExactly setOf(1L)

            verify(exactly = 1) {
                photoBoothLocationRepository.listPointLocations(
                    coordinate = coordinate,
                    radiusInMeters = 1000,
                    brandIds = null,
                )
            }
            verify(exactly = 1) { favoriteMapRepository.findFavoritedLocationIds(userId, listOf(1L)) }
        }

        test("Point 검색 - 결과가 없으면 빈 리스트를 반환하고 즐겨찾기를 조회하지 않는다") {
            // Given
            val coordinate = Coordinate(126.0, 33.0)
            val query = MapQuery.GetPointLocation(
                userId = userId,
                coordinate = coordinate,
                radiusInMeters = 500,
                brandIds = listOf(1L),
            )

            every {
                photoBoothLocationRepository.listPointLocations(
                    coordinate = coordinate,
                    radiusInMeters = 500,
                    brandIds = listOf(1L),
                )
            } returns emptyList()

            // When
            val result = useCase.execute(query)

            // Then
            result.locations.shouldBeEmpty()
            result.favoriteLocationIds.shouldBeEmpty()
            verify(exactly = 0) { favoriteMapRepository.findFavoritedLocationIds(any(), any()) }
        }
    })
