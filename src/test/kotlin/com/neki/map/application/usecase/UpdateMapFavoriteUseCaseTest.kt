package com.neki.map.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.map.application.command.UpdateMapFavoriteCommand
import com.neki.map.application.port.FavoriteMapRepositoryPort
import com.neki.map.application.port.PhotoBoothLocationRepositoryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * fileName       : UpdateMapFavoriteUseCaseTest
 * description    : UpdateMapFavoriteUseCase 단위 테스트
 */
class UpdateMapFavoriteUseCaseTest {

    lateinit var photoBoothLocationRepository: PhotoBoothLocationRepositoryPort
    lateinit var favoriteMapRepository: FavoriteMapRepositoryPort
    lateinit var useCase: UpdateMapFavoriteUseCase

    @BeforeEach
    fun setUp() {
        photoBoothLocationRepository = mockk()
        favoriteMapRepository = mockk()
        useCase = UpdateMapFavoriteUseCase(photoBoothLocationRepository, favoriteMapRepository)
    }

    @Test
    @DisplayName("즐겨찾기 추가 요청 시 add 호출")
    fun `즐겨찾기 추가 요청 시 add 호출`() {
        // Given
        val command = UpdateMapFavoriteCommand(userId = 1L, locationId = 1L, favorite = true)

        every { photoBoothLocationRepository.existsById(1L) } returns true
        every { favoriteMapRepository.add(1L, 1L) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteMapRepository.add(1L, 1L) }
        verify(exactly = 0) { favoriteMapRepository.delete(any(), any()) }
    }

    @Test
    @DisplayName("즐겨찾기 해제 요청 시 delete 호출")
    fun `즐겨찾기 해제 요청 시 delete 호출`() {
        // Given
        val command = UpdateMapFavoriteCommand(userId = 1L, locationId = 1L, favorite = false)

        every { photoBoothLocationRepository.existsById(1L) } returns true
        every { favoriteMapRepository.delete(1L, 1L) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteMapRepository.delete(1L, 1L) }
        verify(exactly = 0) { favoriteMapRepository.add(any(), any()) }
    }

    @Test
    @DisplayName("포토부스 위치가 존재하지 않는 경우 NOT_FOUND 예외 발생")
    fun `포토부스 위치가 존재하지 않는 경우 NOT_FOUND 예외 발생`() {
        // Given
        val command = UpdateMapFavoriteCommand(userId = 1L, locationId = 99L, favorite = true)

        every { photoBoothLocationRepository.existsById(99L) } returns false

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { favoriteMapRepository.add(any(), any()) }
        verify(exactly = 0) { favoriteMapRepository.delete(any(), any()) }
    }
}
