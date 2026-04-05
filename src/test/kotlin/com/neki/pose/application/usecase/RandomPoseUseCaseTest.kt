package com.neki.pose.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.command.GetRandomPoseCommand
import com.neki.pose.application.contract.MediaStorageInfo
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.RandomGeneratorPort
import com.neki.pose.application.port.ScrapPoseRepositoryPort
import com.neki.pose.domain.HeadCount
import com.neki.testfixture.aPose
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class RandomPoseUseCaseTest :
    FunSpec({

        lateinit var poseRepository: PoseRepositoryPort
        lateinit var scrapPoseRepository: ScrapPoseRepositoryPort
        lateinit var mediaClient: MediaClientPort
        lateinit var randomGenerator: RandomGeneratorPort
        lateinit var useCase: RandomPoseUseCase

        beforeTest {
            poseRepository = mockk()
            scrapPoseRepository = mockk()
            mediaClient = mockk()
            randomGenerator = mockk()
            useCase = RandomPoseUseCase(poseRepository, scrapPoseRepository, mediaClient, randomGenerator)
        }

        fun makeCommand(
            userId: Long = 1L,
            headCount: HeadCount = HeadCount.TWO,
            excludeIds: List<Long> = emptyList(),
        ): GetRandomPoseCommand = GetRandomPoseCommand(userId = userId, headCount = headCount, excludeIds = excludeIds)

        fun makeMediaStorageInfo(mediaId: Long): MediaStorageInfo = MediaStorageInfo(
            mediaId = mediaId,
            storageKey = "pose/image-$mediaId.jpg",
            contentType = "image/jpeg",
            width = 800,
            height = 600,
        )

        test("정상 - 랜덤 포즈 반환") {
            // Given
            val command = makeCommand()
            val pose = aPose(id = 10L, mediaId = 101L, headCount = HeadCount.TWO)
            pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)

            every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 5L
            every { randomGenerator.nextLong(5L) } returns 2L
            every { poseRepository.findPoseByOffset(2L, HeadCount.TWO, emptyList()) } returns pose
            every { scrapPoseRepository.existsOwnedPoseScrap(1L, 10L) } returns false
            every { mediaClient.getMediaStorageInfo(101L) } returns makeMediaStorageInfo(101L)

            // When
            val result = useCase.execute(command)

            // Then
            result.poseId shouldBe 10L
            result.storageKey shouldBe "pose/image-101.jpg"
            result.scrap shouldBe false
            result.headCount shouldBe HeadCount.TWO
        }

        test("포즈 없음 → BusinessException(NO_MORE_RANDOM_POSE)") {
            // Given
            val command = makeCommand()
            every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 0L

            // When / Then
            val ex = shouldThrow<BusinessException> {
                useCase.execute(command)
            }
            ex.resultCode shouldBe ResultCode.NO_MORE_RANDOM_POSE
        }

        test("offset 포즈 미존재 (race condition) - findPoseByOffset null → BusinessException(NO_MORE_RANDOM_POSE)") {
            // Given
            val command = makeCommand()
            every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 3L
            every { randomGenerator.nextLong(3L) } returns 1L
            every { poseRepository.findPoseByOffset(1L, HeadCount.TWO, emptyList()) } returns null

            // When / Then
            val ex = shouldThrow<BusinessException> {
                useCase.execute(command)
            }
            ex.resultCode shouldBe ResultCode.NO_MORE_RANDOM_POSE
        }

        test("count=1 - nextLong(1) → offset=0 → 하나의 포즈 반환") {
            // Given
            val command = makeCommand()
            val pose = aPose(id = 5L, mediaId = 201L, headCount = HeadCount.TWO)
            pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)

            every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 1L
            every { randomGenerator.nextLong(1L) } returns 0L
            every { poseRepository.findPoseByOffset(0L, HeadCount.TWO, emptyList()) } returns pose
            every { scrapPoseRepository.existsOwnedPoseScrap(1L, 5L) } returns true
            every { mediaClient.getMediaStorageInfo(201L) } returns makeMediaStorageInfo(201L)

            // When
            val result = useCase.execute(command)

            // Then
            result.poseId shouldBe 5L
            result.scrap shouldBe true
        }

        test("mediaClient 예외 - 포즈 선택 후 미디어 조회 실패") {
            // Given
            val command = makeCommand()
            val pose = aPose(id = 10L, mediaId = 101L, headCount = HeadCount.TWO)
            pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)

            every { poseRepository.countPoses(HeadCount.TWO, emptyList()) } returns 5L
            every { randomGenerator.nextLong(5L) } returns 2L
            every { poseRepository.findPoseByOffset(2L, HeadCount.TWO, emptyList()) } returns pose
            every { scrapPoseRepository.existsOwnedPoseScrap(1L, 10L) } returns false
            every { mediaClient.getMediaStorageInfo(101L) } throws RuntimeException("미디어 조회 실패")

            // When / Then
            shouldThrow<RuntimeException> {
                useCase.execute(command)
            }
        }
    })
