package com.neki.api.map.application.usecase

import com.neki.api.map.application.GetFavoriteMapsUseCase
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.service.MapService
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
 * fileName       : GetFavoriteMapsUseCaseTest
 * description    : GetFavoriteMapsUseCase 단위 테스트
 */
class GetFavoriteMapsUseCaseTest :
    FunSpec({

        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val userId = 1L

        lateinit var favoriteMapRepository: FavoriteMapRepository
        lateinit var useCase: GetFavoriteMapsUseCase

        beforeTest {
            favoriteMapRepository = mockk()
            useCase = GetFavoriteMapsUseCase(MapService(favoriteMapRepository, mockk()))
        }

        test("즐겨찾기한 포토부스가 있으면 즐겨찾기한 순서대로 목록을 반환한다") {
            // Given
            val query = MapQuery.GetFavoriteMaps(userId = userId)

            val point1 = geometryFactory.createPoint(Coordinate(127.02, 37.49))
            val point2 = geometryFactory.createPoint(Coordinate(127.03, 37.50))
            // Repository가 즐겨찾기한 순서대로 정렬해 반환
            val locationDtos = listOf(
                PhotoBoothLocationView(
                    id = 2L,
                    brandName = "하루필름",
                    branchName = "홍대점",
                    address = "서울 마포구",
                    location = point2,
                ),
                PhotoBoothLocationView(
                    id = 1L,
                    brandName = "인생네컷",
                    branchName = "강남점",
                    address = "서울 강남구",
                    location = point1,
                ),
            )

            every { favoriteMapRepository.findFavoriteLocationsByUserId(userId) } returns locationDtos

            // When
            val result = useCase.execute(query)

            // Then
            result.locations shouldHaveSize 2
            result.locations[0].id shouldBe 2L
            result.locations[1].id shouldBe 1L

            verify(exactly = 1) { favoriteMapRepository.findFavoriteLocationsByUserId(userId) }
        }

        test("즐겨찾기한 포토부스가 없으면 빈 리스트를 반환한다") {
            // Given
            val query = MapQuery.GetFavoriteMaps(userId = userId)

            every { favoriteMapRepository.findFavoriteLocationsByUserId(userId) } returns emptyList()

            // When
            val result = useCase.execute(query)

            // Then
            result.locations.shouldBeEmpty()
        }
    })
