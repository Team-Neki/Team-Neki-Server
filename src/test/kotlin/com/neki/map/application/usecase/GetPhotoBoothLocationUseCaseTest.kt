package com.neki.map.application.usecase

import com.neki.map.application.command.GetPointLocationCommand
import com.neki.map.application.command.GetPolygonLocationCommand
import com.neki.map.application.contract.PhotoBoothLocationDto
import com.neki.map.application.contract.PhotoBoothLocationWithDistanceDto
import com.neki.map.application.port.PhotoBoothLocationRepositoryPort
import com.neki.testfixture.FakeTransactionRunner
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
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
class GetPhotoBoothLocationUseCaseTest : FunSpec({

    val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    lateinit var photoBoothLocationRepository: PhotoBoothLocationRepositoryPort
    lateinit var useCase: GetPhotoBoothLocationUseCase

    beforeTest {
        photoBoothLocationRepository = mockk()
        useCase = GetPhotoBoothLocationUseCase(photoBoothLocationRepository, FakeTransactionRunner())
    }

    // ── Polygon 검색 ───────────────────────────────────────────────────────────

    test("Polygon 검색 - 결과가 있으면 해당 위치 목록을 반환한다") {
        // Given
        val coordinates = listOf(
            Coordinate(127.0, 37.0),
            Coordinate(127.5, 37.0),
            Coordinate(127.5, 37.5),
            Coordinate(127.0, 37.5),
            Coordinate(127.0, 37.0),
        )
        val command = GetPolygonLocationCommand(coordinates = coordinates, brandIds = null)

        val point1 = geometryFactory.createPoint(Coordinate(127.02, 37.49))
        val point2 = geometryFactory.createPoint(Coordinate(127.03, 37.50))
        val locationDtos = listOf(
            PhotoBoothLocationDto(id = 1L, brandName = "인생네컷", branchName = "강남점", address = "서울 강남구", location = point1),
            PhotoBoothLocationDto(id = 2L, brandName = "하루필름", branchName = "홍대점", address = "서울 마포구", location = point2),
        )

        every {
            photoBoothLocationRepository.listPolygonLocations(
                coordinates = coordinates,
                brandIds = null,
            )
        } returns locationDtos

        // When
        val result = useCase.execute(command)

        // Then
        result.locations shouldHaveSize 2
        result.locations[0].id shouldBe 1L
        result.locations[0].brandName shouldBe "인생네컷"
        result.locations[1].id shouldBe 2L
        result.locations[1].brandName shouldBe "하루필름"

        verify(exactly = 1) {
            photoBoothLocationRepository.listPolygonLocations(
                coordinates = coordinates,
                brandIds = null,
            )
        }
    }

    test("Polygon 검색 - 결과가 없으면 빈 리스트를 반환한다") {
        // Given
        val coordinates = listOf(
            Coordinate(126.0, 36.0),
            Coordinate(126.5, 36.0),
            Coordinate(126.5, 36.5),
            Coordinate(126.0, 36.5),
            Coordinate(126.0, 36.0),
        )
        val command = GetPolygonLocationCommand(coordinates = coordinates, brandIds = listOf(1L, 2L))

        every {
            photoBoothLocationRepository.listPolygonLocations(
                coordinates = coordinates,
                brandIds = listOf(1L, 2L),
            )
        } returns emptyList()

        // When
        val result = useCase.execute(command)

        // Then
        result.locations.shouldBeEmpty()
    }

    // ── Point 검색 ────────────────────────────────────────────────────────────

    test("Point 검색 - 결과가 있으면 거리 정보가 포함된 위치 목록을 반환한다") {
        // Given
        val coordinate = Coordinate(127.0276, 37.4979)
        val command = GetPointLocationCommand(coordinate = coordinate, radiusInMeters = 1000, brandIds = null)

        val point = geometryFactory.createPoint(Coordinate(127.0280, 37.4985))
        val locationDtos = listOf(
            PhotoBoothLocationWithDistanceDto(
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

        // When
        val result = useCase.execute(command)

        // Then
        result.locations shouldHaveSize 1
        result.locations[0].id shouldBe 1L
        result.locations[0].brandName shouldBe "인생네컷"
        result.locations[0].distance shouldBe 350

        verify(exactly = 1) {
            photoBoothLocationRepository.listPointLocations(
                coordinate = coordinate,
                radiusInMeters = 1000,
                brandIds = null,
            )
        }
    }

    test("Point 검색 - 결과가 없으면 빈 리스트를 반환한다") {
        // Given
        val coordinate = Coordinate(126.0, 33.0)
        val command = GetPointLocationCommand(coordinate = coordinate, radiusInMeters = 500, brandIds = listOf(1L))

        every {
            photoBoothLocationRepository.listPointLocations(
                coordinate = coordinate,
                radiusInMeters = 500,
                brandIds = listOf(1L),
            )
        } returns emptyList()

        // When
        val result = useCase.execute(command)

        // Then
        result.locations.shouldBeEmpty()
    }
})
