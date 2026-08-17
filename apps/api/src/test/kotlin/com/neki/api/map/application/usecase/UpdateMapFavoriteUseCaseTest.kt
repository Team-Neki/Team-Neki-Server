package com.neki.api.map.application.usecase

import com.neki.api.map.application.UpdateMapFavoriteUseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.repository.FavoriteMapRepository
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
 * description    : UpdateMapFavoriteUseCase 단위 테스트
 */
class UpdateMapFavoriteUseCaseTest {

    lateinit var photoBoothLocationRepository: PhotoBoothLocationRepository
    lateinit var favoriteMapRepository: FavoriteMapRepository
    lateinit var useCase: UpdateMapFavoriteUseCase

    @BeforeEach
    fun setUp() {
        photoBoothLocationRepository = mockk()
        favoriteMapRepository = mockk()
        useCase = UpdateMapFavoriteUseCase(
            MapService(
                favoriteMapRepository,
                photoBoothLocationRepository,
            ),
        )
    }

    @Test
    @DisplayName("즐겨찾기 추가 요청 시 add 호출")
    fun `즐겨찾기 추가 요청 시 add 호출`() {
        // Given
        val command = MapCommand.UpdateMapFavorite(userId = 1L, locationId = 1L, favorite = true)

        val favoriteMapSlot = slot<FavoriteMap>()
        every { photoBoothLocationRepository.existsById(1L) } returns true
        every { favoriteMapRepository.add(capture(favoriteMapSlot)) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteMapRepository.add(any()) }
        verify(exactly = 0) { favoriteMapRepository.delete(any()) }
        favoriteMapSlot.captured.id.userId shouldBe 1L
        favoriteMapSlot.captured.id.locationId shouldBe 1L
    }

    @Test
    @DisplayName("즐겨찾기 해제 요청 시 delete 호출")
    fun `즐겨찾기 해제 요청 시 delete 호출`() {
        // Given
        val command = MapCommand.UpdateMapFavorite(userId = 1L, locationId = 1L, favorite = false)

        val favoriteMapSlot = slot<FavoriteMap>()
        every { photoBoothLocationRepository.existsById(1L) } returns true
        every { favoriteMapRepository.delete(capture(favoriteMapSlot)) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteMapRepository.delete(any()) }
        verify(exactly = 0) { favoriteMapRepository.add(any()) }
        favoriteMapSlot.captured.id.userId shouldBe 1L
        favoriteMapSlot.captured.id.locationId shouldBe 1L
    }

    @Test
    @DisplayName("포토부스 위치가 존재하지 않는 경우 NOT_FOUND 예외 발생")
    fun `포토부스 위치가 존재하지 않는 경우 NOT_FOUND 예외 발생`() {
        // Given
        val command = MapCommand.UpdateMapFavorite(userId = 1L, locationId = 99L, favorite = true)

        every { photoBoothLocationRepository.existsById(99L) } returns false

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { favoriteMapRepository.add(any()) }
        verify(exactly = 0) { favoriteMapRepository.delete(any()) }
    }
}
