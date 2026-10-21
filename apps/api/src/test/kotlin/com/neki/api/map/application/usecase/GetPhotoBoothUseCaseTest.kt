package com.neki.api.map.application.usecase

import com.neki.api.map.application.GetPhotoBoothUseCase
import com.neki.api.map.application.dto.MapResult
import com.neki.api.testfixture.FakeTransactionRunner
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.models.PhotoBoothWithDistance
import com.neki.domain.map.repository.PhotoBoothRepository
import com.neki.domain.map.service.MapService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

/**
 * fileName       : GetPhotoBoothUseCaseTest
 * description    : GetPhotoBoothUseCase(지도 V2) 단위 테스트
 */
class GetPhotoBoothUseCaseTest :
    FunSpec({

        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val userId = 1L
        val key1 = PhotoBoothKey("LIFEFOURCUTS", "1")
        val key2 = PhotoBoothKey("HARUFILM", "2")

        lateinit var photoBoothRepository: PhotoBoothRepository
        lateinit var mapService: MapService
        lateinit var useCase: GetPhotoBoothUseCase

        beforeTest {
            photoBoothRepository = mockk()
            mapService = mockk()
            useCase = GetPhotoBoothUseCase(photoBoothRepository, mapService, FakeTransactionRunner())
        }

        fun view(key: PhotoBoothKey) = PhotoBoothView(
            key = key,
            brandName = "인생네컷",
            branchName = "인생네컷 강남점",
            address = "서울 강남구",
            location = geometryFactory.createPoint(Coordinate(127.02, 37.49)),
        )

        test("Polygon 검색 - 결과가 있으면 부스 목록과 즐겨찾기한 키를 반환한다") {
            // Given
            val coordinates = listOf(Coordinate(127.0, 37.0), Coordinate(127.5, 37.0), Coordinate(127.0, 37.0))
            val query = MapQuery.GetPolygonLocation(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothRepository.listPolygonPhotoBooths(coordinates, null) } returns
                listOf(view(key1), view(key2))
            every { mapService.getFavoritedPhotoBoothKeys(userId) } returns setOf(key1)

            // When
            val result: MapResult.GetPolygonPhotoBooths = useCase.execute(query)

            // Then
            result.photoBooths.map { it.key } shouldBe listOf(key1, key2)
            result.favoriteKeys shouldBe setOf(key1)
        }

        test("Polygon 검색 - 결과가 없으면 즐겨찾기를 조회하지 않는다") {
            // Given
            val coordinates = listOf(Coordinate(126.0, 36.0), Coordinate(126.5, 36.0), Coordinate(126.0, 36.0))
            val query = MapQuery.GetPolygonLocation(userId = userId, coordinates = coordinates, brandIds = listOf(1L))

            every { photoBoothRepository.listPolygonPhotoBooths(coordinates, listOf(1L)) } returns emptyList()

            // When
            val result: MapResult.GetPolygonPhotoBooths = useCase.execute(query)

            // Then
            result.photoBooths.shouldBeEmpty()
            result.favoriteKeys.shouldBeEmpty()
            verify(exactly = 0) { mapService.getFavoritedPhotoBoothKeys(any()) }
        }

        test("Point 검색 - 거리와 즐겨찾기한 키를 반환한다") {
            // Given
            val coordinate = Coordinate(127.0276, 37.4979)
            val query = MapQuery.GetPointLocation(userId = userId, coordinate = coordinate, brandIds = null)

            val booth = PhotoBoothWithDistance(
                key = key1,
                brandName = "인생네컷",
                branchName = "인생네컷 강남점",
                address = null,
                location = geometryFactory.createPoint(Coordinate(127.028, 37.4985)),
                distance = 350,
            )
            every { photoBoothRepository.listPointPhotoBooths(coordinate, 1000, null) } returns listOf(booth)
            every { mapService.getFavoritedPhotoBoothKeys(userId) } returns emptySet()

            // When
            val result: MapResult.GetPointPhotoBooths = useCase.execute(query)

            // Then
            result.photoBooths.single().distance shouldBe 350
            result.favoriteKeys.shouldBeEmpty()
        }

        test("키 조회 - 지금 세대에 있는 키만 즐겨찾기 여부와 함께 반환한다") {
            // Given: key2 는 색인 이후 enriched 에서 빠졌다
            val query = MapQuery.GetPhotoBoothsByKeys(userId = userId, keys = listOf(key1, key2))

            every { photoBoothRepository.findExistingKeys(listOf(key1, key2)) } returns setOf(key1)
            every { mapService.getFavoritedPhotoBoothKeys(userId) } returns setOf(key1, key2)

            // When
            val result: MapResult.GetPhotoBoothsByKeys = useCase.execute(query)

            // Then
            result.photoBooths shouldContainExactlyInAnyOrder
                listOf(MapResult.GetPhotoBoothsByKeys.PhotoBooth(key = key1, favorite = true))
        }

        test("키 조회 - 있는 키가 없으면 즐겨찾기를 조회하지 않는다") {
            // Given
            val query = MapQuery.GetPhotoBoothsByKeys(userId = userId, keys = listOf(key1))

            every { photoBoothRepository.findExistingKeys(listOf(key1)) } returns emptySet()

            // When
            val result: MapResult.GetPhotoBoothsByKeys = useCase.execute(query)

            // Then
            result.photoBooths.shouldBeEmpty()
            verify(exactly = 0) { mapService.getFavoritedPhotoBoothKeys(any()) }
        }
    })
