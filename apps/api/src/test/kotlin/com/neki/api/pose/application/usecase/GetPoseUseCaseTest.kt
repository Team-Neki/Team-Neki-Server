package com.neki.api.pose.application.usecase

import com.neki.api.pose.application.GetPoseUseCase
import com.neki.api.testfixture.aPose
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.MediaMetadata
import com.neki.domain.pose.models.PoseWithScrap
import com.neki.domain.pose.repository.PoseRepository
import com.neki.domain.pose.service.PoseScrapService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GetPoseUseCaseTest {

    private lateinit var poseRepository: PoseRepository
    private lateinit var mediaClient: MediaClient
    private lateinit var useCase: GetPoseUseCase

    @BeforeEach
    fun setUp() {
        poseRepository = mockk()
        mediaClient = mockk()
        useCase = GetPoseUseCase(
            PoseScrapService(poseRepository, mockk()),
            mediaClient,
        )
    }

    private fun makePoseWithScrap(id: Long = 1L, mediaId: Long = 101L, scraped: Boolean = false): PoseWithScrap {
        val pose = aPose(id = id, mediaId = mediaId)
        pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        return PoseWithScrap(pose = pose, isScraped = scraped)
    }

    private fun makeMediaMetadata(mediaId: Long = 101L): MediaMetadata = MediaMetadata(
        mediaId = mediaId,
        storageKey = "pose/image-$mediaId.jpg",
        contentType = "image/jpeg",
        width = 800,
        height = 600,
    )

    @Test
    @DisplayName("포즈 상세 조회 - 미디어 매핑")
    fun `포즈 상세 조회 - 미디어 매핑`() {
        // Given
        val query = PoseQuery.GetPose(userId = 1L, poseId = 10L)
        val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
        every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
        every { mediaClient.getMediaMetadata(101L) } returns makeMediaMetadata(101L)

        // When
        val result = useCase.execute(query)

        // Then
        result.poseId shouldBe 10L
        result.storageKey shouldBe "pose/image-101.jpg"
        result.scrap shouldBe false
    }

    @Test
    @DisplayName("포즈 미존재 → BusinessException(NOT_FOUND)")
    fun `포즈 미존재 → BusinessException(NOT_FOUND)`() {
        // Given
        val query = PoseQuery.GetPose(userId = 1L, poseId = 999L)
        every { poseRepository.getOwnedPoseWithScrap(1L, 999L) } returns null

        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(query)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { mediaClient.getMediaMetadata(any<Long>()) }
    }

    @Test
    @DisplayName("미디어 조회 실패 - 예외 전파")
    fun `미디어 조회 실패 - 예외 전파`() {
        // Given
        val query = PoseQuery.GetPose(userId = 1L, poseId = 10L)
        val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
        every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
        every { mediaClient.getMediaMetadata(101L) } throws RuntimeException("미디어 조회 실패")

        // When / Then
        shouldThrow<RuntimeException> {
            useCase.execute(query)
        }
    }
}
