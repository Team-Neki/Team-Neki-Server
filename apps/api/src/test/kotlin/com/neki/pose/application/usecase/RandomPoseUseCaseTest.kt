package com.neki.pose.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.RandomPoseUseCase
import com.neki.pose.client.MediaClient
import com.neki.pose.dto.PoseQuery
import com.neki.pose.external.RandomGenerator
import com.neki.pose.models.HeadCount
import com.neki.pose.models.MediaMetadata
import com.neki.pose.models.ScrapPoseId
import com.neki.pose.repository.PoseRepository
import com.neki.pose.repository.ScrapPoseRepository
import com.neki.pose.service.PoseService
import com.neki.testfixture.aPose
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RandomPoseUseCaseTest {

    private lateinit var poseRepository: PoseRepository
    private lateinit var scrapPoseRepository: ScrapPoseRepository
    private lateinit var mediaClient: MediaClient
    private lateinit var randomGenerator: RandomGenerator
    private lateinit var useCase: RandomPoseUseCase

    @BeforeEach
    fun setUp() {
        poseRepository = mockk()
        scrapPoseRepository = mockk()
        mediaClient = mockk()
        randomGenerator = mockk()
        useCase =
            RandomPoseUseCase(
                PoseService(poseRepository, scrapPoseRepository, mockk(), randomGenerator),
                mediaClient,
            )
    }

    private fun makeQuery(
        userId: Long = 1L,
        headCount: HeadCount = HeadCount.TWO,
        excludeIds: List<Long> = emptyList(),
    ): PoseQuery.GetRandomPose =
        PoseQuery.GetRandomPose(userId = userId, headCount = headCount, excludeIds = excludeIds)

    private fun makeMediaMetadata(mediaId: Long): MediaMetadata = MediaMetadata(
        mediaId = mediaId,
        storageKey = "pose/image-$mediaId.jpg",
        contentType = "image/jpeg",
        width = 800,
        height = 600,
    )

    @Test
    @DisplayName("정상 - 랜덤 포즈 반환")
    fun `정상 - 랜덤 포즈 반환`() {
        // Given
        val query = makeQuery()
        val pose = aPose(id = 10L, mediaId = 101L, headCount = HeadCount.TWO)
        pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)

        every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 5L
        every { randomGenerator.nextLong(5L) } returns 2L
        every { poseRepository.findPoseByOffset(2L, HeadCount.TWO, emptyList()) } returns pose
        every { scrapPoseRepository.existsOwnedPoseScrap(match { it.id == ScrapPoseId(1L, 10L) }) } returns false
        every { mediaClient.getMediaMetadata(101L) } returns makeMediaMetadata(101L)

        // When
        val result = useCase.execute(query)

        // Then
        result.poseId shouldBe 10L
        result.storageKey shouldBe "pose/image-101.jpg"
        result.scrap shouldBe false
        result.headCount shouldBe HeadCount.TWO
    }

    @Test
    @DisplayName("포즈 없음 → BusinessException(NO_MORE_RANDOM_POSE)")
    fun `포즈 없음 → BusinessException(NO_MORE_RANDOM_POSE)`() {
        // Given
        val query = makeQuery()
        every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 0L

        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(query)
        }
        ex.resultCode shouldBe ResultCode.NO_MORE_RANDOM_POSE
    }

    @Test
    @DisplayName("offset 포즈 미존재 (race condition) - findPoseByOffset null → BusinessException(NO_MORE_RANDOM_POSE)")
    fun `offset 포즈 미존재 (race condition) - findPoseByOffset null → BusinessException(NO_MORE_RANDOM_POSE)`() {
        // Given
        val query = makeQuery()
        every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 3L
        every { randomGenerator.nextLong(3L) } returns 1L
        every { poseRepository.findPoseByOffset(1L, HeadCount.TWO, emptyList()) } returns null

        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(query)
        }
        ex.resultCode shouldBe ResultCode.NO_MORE_RANDOM_POSE
    }

    @Test
    @DisplayName("count=1 - nextLong(1) → offset=0 → 하나의 포즈 반환")
    fun `count=1 - nextLong(1) → offset=0 → 하나의 포즈 반환`() {
        // Given
        val query = makeQuery()
        val pose = aPose(id = 5L, mediaId = 201L, headCount = HeadCount.TWO)
        pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)

        every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 1L
        every { randomGenerator.nextLong(1L) } returns 0L
        every { poseRepository.findPoseByOffset(0L, HeadCount.TWO, emptyList()) } returns pose
        every { scrapPoseRepository.existsOwnedPoseScrap(match { it.id == ScrapPoseId(1L, 5L) }) } returns true
        every { mediaClient.getMediaMetadata(201L) } returns makeMediaMetadata(201L)

        // When
        val result = useCase.execute(query)

        // Then
        result.poseId shouldBe 5L
        result.scrap shouldBe true
    }

    @Test
    @DisplayName("mediaClient 예외 - 포즈 선택 후 미디어 조회 실패")
    fun `mediaClient 예외 - 포즈 선택 후 미디어 조회 실패`() {
        // Given
        val query = makeQuery()
        val pose = aPose(id = 10L, mediaId = 101L, headCount = HeadCount.TWO)
        pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)

        every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 5L
        every { randomGenerator.nextLong(5L) } returns 2L
        every { poseRepository.findPoseByOffset(2L, HeadCount.TWO, emptyList()) } returns pose
        every { scrapPoseRepository.existsOwnedPoseScrap(match { it.id == ScrapPoseId(1L, 10L) }) } returns false
        every { mediaClient.getMediaMetadata(101L) } throws RuntimeException("미디어 조회 실패")

        // When / Then
        shouldThrow<RuntimeException> {
            useCase.execute(query)
        }
    }
}
