package com.neki.api.share.infra.client

import com.neki.api.media.application.GetImageByKeyUseCase
import com.neki.api.media.application.dto.MediaResult
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.share.models.SharedImageContent
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ShareMediaClientTest {

    private val getImageByKeyUseCase: GetImageByKeyUseCase = mockk()
    private val client = ShareMediaClient(getImageByKeyUseCase)

    @Test
    @DisplayName("storageKey 로 이미지를 조회해 SharedImageContent.Binary 로 변환한다")
    fun `storageKey 로 이미지를 조회해 Binary 로 변환한다`() {
        // Given
        val bytes = byteArrayOf(1, 2, 3)
        every { getImageByKeyUseCase.execute(MediaQuery.GetImageByKey(objectKey = "photo-booth/a.png")) } returns
            MediaResult.GetImageByKey(bytes, "image/png")

        // When
        val result: SharedImageContent.Binary = client.fetchImage("photo-booth/a.png")

        // Then
        result shouldBe SharedImageContent.Binary(byteArrayOf(1, 2, 3), "image/png")
    }
}
