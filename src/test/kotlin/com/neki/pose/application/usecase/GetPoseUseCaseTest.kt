package com.neki.pose.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.dto.PoseQuery
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.PoseViewCachePort
import com.neki.pose.application.port.dto.MediaContract
import com.neki.pose.application.port.dto.PoseContract
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aPose
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

    private lateinit var poseRepository: PoseRepositoryPort
    private lateinit var mediaClient: MediaClientPort
    private lateinit var poseViewCache: PoseViewCachePort
    private lateinit var transactionRunner: FakeTransactionRunner
    private lateinit var useCase: GetPoseUseCase

    @BeforeEach
    fun setUp() {
        poseRepository = mockk()
        mediaClient = mockk()
        poseViewCache = mockk()
        transactionRunner = FakeTransactionRunner()
        useCase = GetPoseUseCase(poseRepository, mediaClient, poseViewCache, transactionRunner)
    }

    private fun makePoseWithScrap(
        id: Long = 1L,
        mediaId: Long = 101L,
        scraped: Boolean = false,
    ): PoseContract.PoseWithScrap {
        val pose = aPose(id = id, mediaId = mediaId)
        pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        return PoseContract.PoseWithScrap(pose = pose, isScraped = scraped)
    }

    private fun makeMediaStorageInfo(mediaId: Long = 101L): MediaContract.StorageInfo = MediaContract.StorageInfo(
        mediaId = mediaId,
        storageKey = "pose/image-$mediaId.jpg",
        contentType = "image/jpeg",
        width = 800,
        height = 600,
    )

    @Test
    @DisplayName("신규 조회자 (cache miss) - addViewer=true → 조회수 증가 + 미디어 매핑")
    fun `신규 조회자 (cache miss) - addViewer=true → 조회수 증가 + 미디어 매핑`() {
        // Given
        val query = PoseQuery.GetPose(userId = 1L, poseId = 10L)
        val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
        every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
        every { poseViewCache.addViewer(10L, 1L) } returns true // cache miss → 신규 조회자
        every { poseRepository.incrementViewCount(10L) } returns Unit
        every { mediaClient.getMediaStorageInfo(101L) } returns makeMediaStorageInfo(101L)

        // When
        val result = useCase.execute(query)

        // Then
        verify(exactly = 1) { poseRepository.incrementViewCount(10L) }
        result.poseId shouldBe 10L
        result.storageKey shouldBe "pose/image-101.jpg"
        result.scrap shouldBe false
    }

    @Test
    @DisplayName("재방문자 (cache hit) - addViewer=false → 조회수 미증가")
    fun `재방문자 (cache hit) - addViewer=false → 조회수 미증가`() {
        // Given
        val query = PoseQuery.GetPose(userId = 1L, poseId = 10L)
        val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
        every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
        every { poseViewCache.addViewer(10L, 1L) } returns false // cache hit → 재방문자
        every { mediaClient.getMediaStorageInfo(101L) } returns makeMediaStorageInfo(101L)

        // When
        val result = useCase.execute(query)

        // Then
        verify(exactly = 0) { poseRepository.incrementViewCount(any()) }
        result.poseId shouldBe 10L
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
        verify(exactly = 0) { poseViewCache.addViewer(any(), any()) }
        verify(exactly = 0) { mediaClient.getMediaStorageInfo(any()) }
    }

    @Test
    @DisplayName("addViewer 캐시 장애 → 예외 전파")
    fun `addViewer 캐시 장애 → 예외 전파`() {
        // Given
        val query = PoseQuery.GetPose(userId = 1L, poseId = 10L)
        val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
        every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
        every { poseViewCache.addViewer(10L, 1L) } throws RuntimeException("Redis 장애")

        // When / Then
        shouldThrow<RuntimeException> {
            useCase.execute(query)
        }
        verify(exactly = 0) { poseRepository.incrementViewCount(any()) }
    }

    @Test
    @DisplayName("조회수 증가 후 mediaClient 예외 - side effect 발생, 결과 실패")
    fun `조회수 증가 후 mediaClient 예외 - side effect 발생, 결과 실패`() {
        // Given
        val query = PoseQuery.GetPose(userId = 1L, poseId = 10L)
        val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
        every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
        every { poseViewCache.addViewer(10L, 1L) } returns true
        every { poseRepository.incrementViewCount(10L) } returns Unit
        every { mediaClient.getMediaStorageInfo(101L) } throws RuntimeException("미디어 조회 실패")

        // When / Then
        shouldThrow<RuntimeException> {
            useCase.execute(query)
        }
        // 조회수는 이미 증가됨 (side effect 발생)
        verify(exactly = 1) { poseRepository.incrementViewCount(10L) }
    }
}
