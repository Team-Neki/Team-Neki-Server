package com.neki.api.share.infra.client

import com.neki.api.photo.application.GetPhotoUseCase
import com.neki.api.photo.application.dto.PhotoImageResult
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.photo.dto.PhotoImageQuery
import com.neki.domain.share.models.SharePhoto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SharePhotoClientTest {

    private lateinit var getPhotoUseCase: GetPhotoUseCase
    private lateinit var client: SharePhotoClient

    @BeforeEach
    fun setUp() {
        getPhotoUseCase = mockk()
        client = SharePhotoClient(getPhotoUseCase)
    }

    @Test
    @DisplayName("사진 조회 결과를 SharePhoto(photoId, userId, storageKey) 로 변환한다")
    fun `사진 조회 결과를 SharePhoto 로 변환한다`() {
        // Given
        every { getPhotoUseCase.execute(PhotoImageQuery.GetPhoto(userId = 1L, photoId = 2L)) } returns
            PhotoImageResult.GetPhoto(
                photoId = 2L,
                storageKey = "photo-booth/a.jpg",
                favorite = false,
                contentType = "image/jpeg",
                uploadMethod = null,
                createdAt = LocalDateTime.of(2026, 11, 26, 0, 0),
                capturedAt = null,
            )

        // When
        val result: SharePhoto? = client.findSharablePhoto(1L, 2L)

        // Then
        result shouldBe SharePhoto(photoId = 2L, userId = 1L, storageKey = "photo-booth/a.jpg")
    }

    @Test
    @DisplayName("photo/media 쪽 NOT_FOUND 는 null 로 바꾼다")
    fun `NOT_FOUND 는 null`() {
        // Given
        every { getPhotoUseCase.execute(any()) } throws BusinessException(ResultCode.NOT_FOUND)

        // When
        val result: SharePhoto? = client.findSharablePhoto(1L, 2L)

        // Then
        result shouldBe null
    }

    @Test
    @DisplayName("NOT_FOUND 가 아닌 예외는 그대로 던진다")
    fun `다른 예외는 그대로 던진다`() {
        // Given
        every { getPhotoUseCase.execute(any()) } throws BusinessException(ResultCode.ERROR)

        // When
        val ex: BusinessException = shouldThrow<BusinessException> { client.findSharablePhoto(1L, 2L) }

        // Then
        ex.resultCode shouldBe ResultCode.ERROR
    }
}
