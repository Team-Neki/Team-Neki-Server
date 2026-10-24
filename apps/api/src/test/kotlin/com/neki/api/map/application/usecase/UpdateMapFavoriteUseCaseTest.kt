package com.neki.api.map.application.usecase

import com.neki.api.map.application.UpdateMapFavoriteUseCase
import com.neki.api.testfixture.aPhotoBoothLocation
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.models.FavoritePhotoBooth
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.FavoritePhotoBoothRepository
import com.neki.domain.map.repository.PhotoBoothLocationRepository
import com.neki.domain.map.service.MapService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * fileName       : UpdateMapFavoriteUseCaseTest
 * description    : UpdateMapFavoriteUseCase(지도 V1) 단위 테스트
 */
class UpdateMapFavoriteUseCaseTest {

    lateinit var photoBoothLocationRepository: PhotoBoothLocationRepository
    lateinit var favoriteMapRepository: FavoriteMapRepository
    lateinit var favoritePhotoBoothRepository: FavoritePhotoBoothRepository
    lateinit var useCase: UpdateMapFavoriteUseCase

    private val key = PhotoBoothKey(platform = "PHOTOISM", idx = "1768232303")

    /** stores-sync 가 만든 지점. 원천 키가 있다 */
    private val collectedLocation: PhotoBoothLocation = PhotoBoothLocation(
        id = 1L,
        mapId = key.toBoothId(),
        brandId = 1L,
        branchName = "포토이즘 강남점",
        address = "서울 강남구",
        location = aPhotoBoothLocation().location,
        sourcePlatform = key.platform,
        sourceIdx = key.idx,
    )

    /** 어드민이 직접 넣은 지점. 원천 키가 없다 */
    private val keylessLocation: PhotoBoothLocation = aPhotoBoothLocation(id = 2L)

    @BeforeEach
    fun setUp() {
        photoBoothLocationRepository = mockk()
        favoriteMapRepository = mockk()
        favoritePhotoBoothRepository = mockk()
        useCase = UpdateMapFavoriteUseCase(
            MapService(
                favoriteMapRepository,
                favoritePhotoBoothRepository,
                photoBoothLocationRepository,
                mockk(),
            ),
        )
    }

    @Test
    @DisplayName("원천 키가 있는 위치를 즐겨찾기하면 포토부스 키로 새 테이블에 쌓는다")
    fun `원천 키가 있는 위치를 즐겨찾기하면 포토부스 키로 새 테이블에 쌓는다`() {
        // Given
        val command = MapCommand.UpdateMapFavorite(userId = 1L, locationId = 1L, favorite = true)

        val favoriteSlot = slot<FavoritePhotoBooth>()
        every { photoBoothLocationRepository.findById(1L) } returns collectedLocation
        every { favoritePhotoBoothRepository.add(capture(favoriteSlot)) } just Runs

        // When
        useCase.execute(command)

        // Then
        favoriteSlot.captured.id.userId shouldBe 1L
        favoriteSlot.captured.id.key() shouldBe key
        verify(exactly = 0) { favoriteMapRepository.add(any()) }
    }

    @Test
    @DisplayName("원천 키가 없는 위치를 즐겨찾기하면 기존 테이블에 쌓는다")
    fun `원천 키가 없는 위치를 즐겨찾기하면 기존 테이블에 쌓는다`() {
        // Given
        val command = MapCommand.UpdateMapFavorite(userId = 1L, locationId = 2L, favorite = true)

        val favoriteMapSlot = slot<FavoriteMap>()
        every { photoBoothLocationRepository.findById(2L) } returns keylessLocation
        every { favoriteMapRepository.add(capture(favoriteMapSlot)) } just Runs

        // When
        useCase.execute(command)

        // Then
        favoriteMapSlot.captured.id.userId shouldBe 1L
        favoriteMapSlot.captured.id.locationId shouldBe 2L
        verify(exactly = 0) { favoritePhotoBoothRepository.add(any()) }
    }

    @Test
    @DisplayName("즐겨찾기 해제 요청 시 두 테이블에서 모두 지운다")
    fun `즐겨찾기 해제 요청 시 두 테이블에서 모두 지운다`() {
        // Given
        val command = MapCommand.UpdateMapFavorite(userId = 1L, locationId = 1L, favorite = false)

        val favoriteMapSlot = slot<FavoriteMap>()
        every { photoBoothLocationRepository.findById(1L) } returns collectedLocation
        every { favoriteMapRepository.delete(capture(favoriteMapSlot)) } just Runs
        every { favoritePhotoBoothRepository.delete(1L, key) } just Runs

        // When
        useCase.execute(command)

        // Then
        favoriteMapSlot.captured.id.locationId shouldBe 1L
        verify(exactly = 1) { favoritePhotoBoothRepository.delete(1L, key) }
        verify(exactly = 0) { favoriteMapRepository.add(any()) }
        verify(exactly = 0) { favoritePhotoBoothRepository.add(any()) }
    }

    @Test
    @DisplayName("원천 키가 없는 위치의 즐겨찾기 해제는 기존 테이블에서만 지운다")
    fun `원천 키가 없는 위치의 즐겨찾기 해제는 기존 테이블에서만 지운다`() {
        // Given
        val command = MapCommand.UpdateMapFavorite(userId = 1L, locationId = 2L, favorite = false)

        every { photoBoothLocationRepository.findById(2L) } returns keylessLocation
        every { favoriteMapRepository.delete(any()) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteMapRepository.delete(any()) }
        verify(exactly = 0) { favoritePhotoBoothRepository.delete(any(), any()) }
    }

    @Test
    @DisplayName("포토부스 위치가 존재하지 않는 경우 NOT_FOUND 예외 발생")
    fun `포토부스 위치가 존재하지 않는 경우 NOT_FOUND 예외 발생`() {
        // Given
        val command = MapCommand.UpdateMapFavorite(userId = 1L, locationId = 99L, favorite = true)

        every { photoBoothLocationRepository.findById(99L) } returns null

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { favoriteMapRepository.add(any()) }
        verify(exactly = 0) { favoritePhotoBoothRepository.add(any()) }
    }
}
