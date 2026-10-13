package com.neki.api.map.application.usecase

import com.neki.api.map.application.GetFavoritePhotoBoothsUseCase
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.FavoritedPhotoBooth
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.FavoritePhotoBoothRepository
import com.neki.domain.map.repository.PhotoBoothRepository
import com.neki.domain.map.service.MapService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import java.time.LocalDateTime

/**
 * fileName       : GetFavoritePhotoBoothsUseCaseTest
 * description    : GetFavoritePhotoBoothsUseCase(지도 V2) 단위 테스트. 두 즐겨찾기 테이블을 키로 합친다
 */
class GetFavoritePhotoBoothsUseCaseTest :
    FunSpec({

        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val userId = 1L
        val query = MapQuery.GetFavoriteMaps(userId = userId)
        val keyA = PhotoBoothKey("PHOTOISM", "a")
        val keyB = PhotoBoothKey("PHOTOISM", "b")
        val keyC = PhotoBoothKey("PHOTOISM", "c")

        fun favorited(key: PhotoBoothKey, minute: Int) =
            FavoritedPhotoBooth(key, LocalDateTime.of(2026, 10, 13, 12, minute))

        fun view(key: PhotoBoothKey) = PhotoBoothView(
            key = key,
            brandName = "포토이즘",
            branchName = "포토이즘 ${key.idx}점",
            address = null,
            location = geometryFactory.createPoint(Coordinate(127.02, 37.49)),
        )

        lateinit var favoriteMapRepository: FavoriteMapRepository
        lateinit var favoritePhotoBoothRepository: FavoritePhotoBoothRepository
        lateinit var photoBoothRepository: PhotoBoothRepository
        lateinit var useCase: GetFavoritePhotoBoothsUseCase

        beforeTest {
            favoriteMapRepository = mockk()
            favoritePhotoBoothRepository = mockk()
            photoBoothRepository = mockk()
            useCase = GetFavoritePhotoBoothsUseCase(
                MapService(favoriteMapRepository, favoritePhotoBoothRepository, mockk(), photoBoothRepository),
            )
        }

        test("두 테이블의 즐겨찾기를 키로 합쳐 최근 순으로 반환하고, 같은 부스는 한 번만 최근 시각 기준이다") {
            // Given: 새 테이블 a(10분), b(20분) / 기존 테이블 a(30분), c(5분)
            every { favoritePhotoBoothRepository.findFavoritesByUserId(userId) } returns
                listOf(favorited(keyA, 10), favorited(keyB, 20))
            every { favoriteMapRepository.findFavoritedPhotoBooths(userId) } returns
                listOf(favorited(keyA, 30), favorited(keyC, 5))
            every { photoBoothRepository.findByKeys(listOf(keyA, keyB, keyC)) } returns
                listOf(view(keyC), view(keyB), view(keyA))

            // When
            val result = useCase.execute(query)

            // Then
            result.photoBooths.map { it.key } shouldBe listOf(keyA, keyB, keyC)
        }

        test("지금 세대에 없는 부스는 빠진다") {
            // Given
            every { favoritePhotoBoothRepository.findFavoritesByUserId(userId) } returns
                listOf(favorited(keyA, 10), favorited(keyB, 20))
            every { favoriteMapRepository.findFavoritedPhotoBooths(userId) } returns emptyList()
            every { photoBoothRepository.findByKeys(listOf(keyB, keyA)) } returns listOf(view(keyA))

            // When
            val result = useCase.execute(query)

            // Then
            result.photoBooths.map { it.key } shouldBe listOf(keyA)
        }

        test("즐겨찾기가 없으면 부스를 조회하지 않고 빈 리스트를 반환한다") {
            // Given
            every { favoritePhotoBoothRepository.findFavoritesByUserId(userId) } returns emptyList()
            every { favoriteMapRepository.findFavoritedPhotoBooths(userId) } returns emptyList()

            // When
            val result = useCase.execute(query)

            // Then
            result.photoBooths.shouldBeEmpty()
            verify(exactly = 0) { photoBoothRepository.findByKeys(any()) }
        }
    })
