package com.neki.api.map.application.usecase

import com.neki.api.map.application.UpdatePhotoBoothFavoriteUseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.models.FavoritePhotoBooth
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.FavoritePhotoBoothRepository
import com.neki.domain.map.repository.PhotoBoothRepository
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
 * fileName       : UpdatePhotoBoothFavoriteUseCaseTest
 * description    : UpdatePhotoBoothFavoriteUseCase(지도 V2) 단위 테스트
 */
class UpdatePhotoBoothFavoriteUseCaseTest {

    lateinit var favoriteMapRepository: FavoriteMapRepository
    lateinit var favoritePhotoBoothRepository: FavoritePhotoBoothRepository
    lateinit var photoBoothRepository: PhotoBoothRepository
    lateinit var useCase: UpdatePhotoBoothFavoriteUseCase

    private val key = PhotoBoothKey(platform = "BROOM_STUDIO", idx = "1768232303")

    @BeforeEach
    fun setUp() {
        favoriteMapRepository = mockk()
        favoritePhotoBoothRepository = mockk()
        photoBoothRepository = mockk()
        useCase = UpdatePhotoBoothFavoriteUseCase(
            MapService(favoriteMapRepository, favoritePhotoBoothRepository, mockk(), photoBoothRepository),
        )
    }

    @Test
    @DisplayName("즐겨찾기 추가 요청 시 새 테이블에 쌓는다")
    fun `즐겨찾기 추가 요청 시 새 테이블에 쌓는다`() {
        // Given
        val command = MapCommand.UpdatePhotoBoothFavorite(userId = 1L, key = key, favorite = true)

        val favoriteSlot = slot<FavoritePhotoBooth>()
        every { photoBoothRepository.existsByKey(key) } returns true
        every { favoritePhotoBoothRepository.add(capture(favoriteSlot)) } just Runs

        // When
        useCase.execute(command)

        // Then
        favoriteSlot.captured.id.userId shouldBe 1L
        favoriteSlot.captured.id.key() shouldBe key
        verify(exactly = 0) { favoriteMapRepository.add(any()) }
    }

    @Test
    @DisplayName("즐겨찾기 해제 요청 시 기존 테이블의 같은 원천 키 즐겨찾기도 함께 지운다. 지금 세대에 없는 부스여도 지운다")
    fun `즐겨찾기 해제 요청 시 기존 테이블의 같은 원천 키 즐겨찾기도 함께 지운다`() {
        // Given
        val command = MapCommand.UpdatePhotoBoothFavorite(userId = 1L, key = key, favorite = false)

        every { favoritePhotoBoothRepository.delete(1L, key) } just Runs
        every { favoriteMapRepository.deleteByPhotoBoothKey(1L, key) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoritePhotoBoothRepository.delete(1L, key) }
        verify(exactly = 1) { favoriteMapRepository.deleteByPhotoBoothKey(1L, key) }
        verify(exactly = 0) { favoritePhotoBoothRepository.add(any()) }
        verify(exactly = 0) { photoBoothRepository.existsByKey(any()) }
    }

    @Test
    @DisplayName("지금 세대에 없는 부스면 NOT_FOUND 예외 발생")
    fun `지금 세대에 없는 부스면 NOT_FOUND 예외 발생`() {
        // Given
        val command = MapCommand.UpdatePhotoBoothFavorite(userId = 1L, key = key, favorite = true)

        every { photoBoothRepository.existsByKey(key) } returns false

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { favoritePhotoBoothRepository.add(any()) }
    }
}
