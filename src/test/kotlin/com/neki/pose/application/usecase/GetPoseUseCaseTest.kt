package com.neki.pose.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.command.GetPoseCommand
import com.neki.pose.application.contract.MediaStorageInfo
import com.neki.pose.application.contract.PoseWithScrap
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.PoseViewCachePort
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aPose
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class GetPoseUseCaseTest :
    FunSpec({

        lateinit var poseRepository: PoseRepositoryPort
        lateinit var mediaClient: MediaClientPort
        lateinit var poseViewCache: PoseViewCachePort
        lateinit var transactionRunner: FakeTransactionRunner
        lateinit var useCase: GetPoseUseCase

        beforeTest {
            poseRepository = mockk()
            mediaClient = mockk()
            poseViewCache = mockk()
            transactionRunner = FakeTransactionRunner()
            useCase = GetPoseUseCase(poseRepository, mediaClient, poseViewCache, transactionRunner)
        }

        fun makePoseWithScrap(id: Long = 1L, mediaId: Long = 101L, scraped: Boolean = false): PoseWithScrap {
            val pose = aPose(id = id, mediaId = mediaId)
            pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
            return PoseWithScrap(pose = pose, isScraped = scraped)
        }

        fun makeMediaStorageInfo(mediaId: Long = 101L): MediaStorageInfo = MediaStorageInfo(
            mediaId = mediaId,
            storageKey = "pose/image-$mediaId.jpg",
            contentType = "image/jpeg",
            width = 800,
            height = 600,
        )

        test("신규 조회자 (cache miss) - addViewer=true → 조회수 증가 + 미디어 매핑") {
            // Given
            val command = GetPoseCommand(userId = 1L, poseId = 10L)
            val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
            every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
            every { poseViewCache.addViewer(10L, 1L) } returns true // cache miss → 신규 조회자
            every { poseRepository.incrementViewCount(10L) } returns Unit
            every { mediaClient.getMediaStorageInfo(101L) } returns makeMediaStorageInfo(101L)

            // When
            val result = useCase.execute(command)

            // Then
            verify(exactly = 1) { poseRepository.incrementViewCount(10L) }
            result.poseId shouldBe 10L
            result.storageKey shouldBe "pose/image-101.jpg"
            result.scrap shouldBe false
        }

        test("재방문자 (cache hit) - addViewer=false → 조회수 미증가") {
            // Given
            val command = GetPoseCommand(userId = 1L, poseId = 10L)
            val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
            every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
            every { poseViewCache.addViewer(10L, 1L) } returns false // cache hit → 재방문자
            every { mediaClient.getMediaStorageInfo(101L) } returns makeMediaStorageInfo(101L)

            // When
            val result = useCase.execute(command)

            // Then
            verify(exactly = 0) { poseRepository.incrementViewCount(any()) }
            result.poseId shouldBe 10L
        }

        test("포즈 미존재 → BusinessException(NOT_FOUND)") {
            // Given
            val command = GetPoseCommand(userId = 1L, poseId = 999L)
            every { poseRepository.getOwnedPoseWithScrap(1L, 999L) } returns null

            // When / Then
            val ex = shouldThrow<BusinessException> {
                useCase.execute(command)
            }
            ex.resultCode shouldBe ResultCode.NOT_FOUND
            verify(exactly = 0) { poseViewCache.addViewer(any(), any()) }
            verify(exactly = 0) { mediaClient.getMediaStorageInfo(any()) }
        }

        test("addViewer 캐시 장애 → 예외 전파") {
            // Given
            val command = GetPoseCommand(userId = 1L, poseId = 10L)
            val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
            every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
            every { poseViewCache.addViewer(10L, 1L) } throws RuntimeException("Redis 장애")

            // When / Then
            shouldThrow<RuntimeException> {
                useCase.execute(command)
            }
            verify(exactly = 0) { poseRepository.incrementViewCount(any()) }
        }

        test("조회수 증가 후 mediaClient 예외 - side effect 발생, 결과 실패") {
            // Given
            val command = GetPoseCommand(userId = 1L, poseId = 10L)
            val poseWithScrap = makePoseWithScrap(id = 10L, mediaId = 101L)
            every { poseRepository.getOwnedPoseWithScrap(1L, 10L) } returns poseWithScrap
            every { poseViewCache.addViewer(10L, 1L) } returns true
            every { poseRepository.incrementViewCount(10L) } returns Unit
            every { mediaClient.getMediaStorageInfo(101L) } throws RuntimeException("미디어 조회 실패")

            // When / Then
            shouldThrow<RuntimeException> {
                useCase.execute(command)
            }
            // 조회수는 이미 증가됨 (side effect 발생)
            verify(exactly = 1) { poseRepository.incrementViewCount(10L) }
        }
    })
