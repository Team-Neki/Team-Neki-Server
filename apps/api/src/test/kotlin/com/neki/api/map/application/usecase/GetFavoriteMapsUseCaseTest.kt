package com.neki.api.map.application.usecase

import com.neki.api.map.application.GetFavoriteMapsUseCase
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.FavoritedLocation
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.FavoritePhotoBoothRepository
import com.neki.domain.map.service.MapService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import java.time.LocalDateTime

/**
 * fileName       : GetFavoriteMapsUseCaseTest
 * description    : GetFavoriteMapsUseCase(지도 V1) 단위 테스트. 두 즐겨찾기 테이블을 합친다
 */
class GetFavoriteMapsUseCaseTest :
    FunSpec({

        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val userId = 1L
        val query = MapQuery.GetFavoriteMaps(userId = userId)

        fun location(id: Long) = PhotoBoothLocationView(
            id = id,
            brandName = "포토이즘",
            branchName = "포토이즘 ${id}호점",
            address = "서울 강남구",
            location = geometryFactory.createPoint(Coordinate(127.02, 37.49)),
        )

        fun favorited(id: Long, minute: Int) =
            FavoritedLocation(location(id), LocalDateTime.of(2026, 10, 13, 12, minute))

        lateinit var favoriteMapRepository: FavoriteMapRepository
        lateinit var favoritePhotoBoothRepository: FavoritePhotoBoothRepository
        lateinit var useCase: GetFavoriteMapsUseCase

        beforeTest {
            favoriteMapRepository = mockk()
            favoritePhotoBoothRepository = mockk()
            useCase = GetFavoriteMapsUseCase(
                MapService(favoriteMapRepository, favoritePhotoBoothRepository, mockk(), mockk()),
            )
        }

        test("두 테이블의 즐겨찾기를 합쳐 최근 즐겨찾기한 순서대로 반환한다") {
            // Given: 기존 테이블에 1(10분), 3(30분), 새 테이블에 2(20분)
            every { favoriteMapRepository.findFavoriteLocationsByUserId(userId) } returns
                listOf(favorited(1L, 10), favorited(3L, 30))
            every { favoritePhotoBoothRepository.findFavoriteLocationsByUserId(userId) } returns
                listOf(favorited(2L, 20))

            // When
            val result = useCase.execute(query)

            // Then
            result.locations.map { it.id } shouldBe listOf(3L, 2L, 1L)
        }

        test("같은 위치를 두 테이블에서 즐겨찾기했으면 한 번만, 최근 시각 기준으로 반환한다") {
            // Given: 1 은 기존 10분, 새 40분. 2 는 새 20분
            every { favoriteMapRepository.findFavoriteLocationsByUserId(userId) } returns listOf(favorited(1L, 10))
            every { favoritePhotoBoothRepository.findFavoriteLocationsByUserId(userId) } returns
                listOf(favorited(1L, 40), favorited(2L, 20))

            // When
            val result = useCase.execute(query)

            // Then
            result.locations.map { it.id } shouldBe listOf(1L, 2L)
        }

        test("즐겨찾기한 포토부스가 없으면 빈 리스트를 반환한다") {
            // Given
            every { favoriteMapRepository.findFavoriteLocationsByUserId(userId) } returns emptyList()
            every { favoritePhotoBoothRepository.findFavoriteLocationsByUserId(userId) } returns emptyList()

            // When
            val result = useCase.execute(query)

            // Then
            result.locations.shouldBeEmpty()
        }
    })
