package com.neki.api.pose.application.usecase

import com.neki.api.pose.application.BulkUploadPosesUseCase
import com.neki.api.pose.application.dto.PoseResult
import com.neki.api.testfixture.FakeTransactionRunner
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.models.BulkFailReason
import com.neki.domain.pose.models.BulkSkipReason
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseImageFile
import com.neki.domain.pose.models.PoseImageStoreOutcome
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.pose.repository.PoseRepository
import com.neki.domain.pose.service.PoseService
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong

class BulkUploadPosesUseCaseTest {

    private lateinit var mediaClient: MediaClient
    private lateinit var poseRepository: PoseRepository
    private lateinit var useCase: BulkUploadPosesUseCase

    private val poseIdSequence = AtomicLong(1000L)

    @BeforeEach
    fun setUp() {
        mediaClient = mockk()
        poseRepository = mockk()
        useCase = BulkUploadPosesUseCase(PoseService(poseRepository), mediaClient, FakeTransactionRunner())

        every { poseRepository.findExistingSourceKeys(any()) } returns emptySet()
    }

    private fun row(line: Int, key: String, headCount: String = "TWO", tag: String = "friend") =
        PoseCommand.BulkUploadPoses.Row(lineNumber = line, sourceKey = key, headCount = headCount, tag = tag)

    private fun image(name: String) = PoseImageFile(fileName = name, readBytes = { byteArrayOf(1, 2, 3) })

    private fun command(rows: List<PoseCommand.BulkUploadPoses.Row>, images: List<PoseImageFile>) =
        PoseCommand.BulkUploadPoses(userId = 1L, rows = rows, images = images)

    private fun stubSaveAssignsId() {
        every { poseRepository.saveAll(any()) } answers {
            firstArg<List<Pose>>().map {
                Pose(
                    id = poseIdSequence.incrementAndGet(),
                    userId = it.userId,
                    mediaId = it.mediaId,
                    headCount = it.headCount,
                    relation = it.relation,
                    sourceKey = it.sourceKey,
                )
            }
        }
    }

    @Test
    @DisplayName("전부 성공 - 행마다 이미지 저장 후 포즈를 저장하고 CSV 순서대로 created 에 담는다")
    fun allSucceed() {
        // Given
        val cmd = command(
            rows = listOf(row(2, "friend_001"), row(3, "ssum_001", tag = "ssum"), row(4, "solo_001", "ONE", "")),
            images = listOf(image("friend_001.png"), image("ssum_001.png"), image("solo_001.png")),
        )
        every { mediaClient.storePoseImage(1L, any()) } returnsMany listOf(
            PoseImageStoreOutcome.Stored(11L),
            PoseImageStoreOutcome.Stored(12L),
            PoseImageStoreOutcome.Stored(13L),
        )
        stubSaveAssignsId()

        // When
        val result: PoseResult.BulkUploadPoses = useCase.execute(cmd)

        // Then
        result.totalRows shouldBe 3
        result.created.map { it.sourceKey } shouldBe listOf("friend_001", "ssum_001", "solo_001")
        result.created.map { it.mediaId } shouldBe listOf(11L, 12L, 13L)
        result.created.map { it.relation } shouldBe listOf(PoseRelation.FRIEND, PoseRelation.SSUM, null)
        result.created.map { it.headCount } shouldBe listOf(HeadCount.TWO, HeadCount.TWO, HeadCount.ONE)
        result.skipped shouldBe emptyList()
        result.failed shouldBe emptyList()
        verify(exactly = 3) { poseRepository.saveAll(any()) }
        verify(exactly = 0) { mediaClient.discardPoseImage(any(), any()) }
    }

    @Test
    @DisplayName("이미지 저장 실패 - failed 로 보고하고 discard 하지 않으며 다음 행은 계속한다")
    fun storageFailure() {
        // Given
        val cmd = command(
            rows = listOf(row(2, "a"), row(3, "b")),
            images = listOf(image("a.png"), image("b.png")),
        )
        every { mediaClient.storePoseImage(1L, any()) } returnsMany listOf(
            PoseImageStoreOutcome.Failed(BulkFailReason.STORAGE_FAILED),
            PoseImageStoreOutcome.Stored(12L),
        )
        stubSaveAssignsId()

        // When
        val result = useCase.execute(cmd)

        // Then
        result.failed shouldBe listOf(PoseResult.BulkUploadPoses.Failed("a", 2, BulkFailReason.STORAGE_FAILED))
        result.created.map { it.sourceKey } shouldBe listOf("b")
        verify(exactly = 0) { mediaClient.discardPoseImage(any(), any()) }
        verify(exactly = 1) { poseRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("포즈 저장 실패, 재조회에 키 없음 - discard 후 SAVE_FAILED")
    fun saveFailureWithoutRace() {
        // Given
        val cmd = command(rows = listOf(row(2, "a")), images = listOf(image("a.png")))
        every { mediaClient.storePoseImage(1L, any()) } returns PoseImageStoreOutcome.Stored(11L)
        every { poseRepository.saveAll(any()) } throws RuntimeException("db down")
        every { mediaClient.discardPoseImage(1L, 11L) } just Runs

        // When
        val result = useCase.execute(cmd)

        // Then
        result.failed shouldBe listOf(PoseResult.BulkUploadPoses.Failed("a", 2, BulkFailReason.SAVE_FAILED))
        result.created shouldBe emptyList()
        verify(exactly = 1) { mediaClient.discardPoseImage(1L, 11L) }
    }

    @Test
    @DisplayName("포즈 저장 실패, 재조회에 키 있음(동시 등록) - discard 후 ALREADY_REGISTERED")
    fun saveFailureByRace() {
        // Given
        val cmd = command(rows = listOf(row(2, "a")), images = listOf(image("a.png")))
        every { poseRepository.findExistingSourceKeys(any()) } returnsMany listOf(emptySet(), setOf("a"))
        every { mediaClient.storePoseImage(1L, any()) } returns PoseImageStoreOutcome.Stored(11L)
        every { poseRepository.saveAll(any()) } throws RuntimeException("unique violation")
        every { mediaClient.discardPoseImage(1L, 11L) } just Runs

        // When
        val result = useCase.execute(cmd)

        // Then
        result.skipped shouldBe listOf(PoseResult.BulkUploadPoses.Skipped("a", 2, BulkSkipReason.ALREADY_REGISTERED))
        result.failed shouldBe emptyList()
        verify(exactly = 1) { mediaClient.discardPoseImage(1L, 11L) }
    }

    @Test
    @DisplayName("discard 가 예외를 던져도 다음 행을 계속 처리하고 결과를 돌려준다")
    fun discardFailureContinues() {
        // Given
        val cmd = command(
            rows = listOf(row(2, "a"), row(3, "b")),
            images = listOf(image("a.png"), image("b.png")),
        )
        every { mediaClient.storePoseImage(1L, any()) } returnsMany listOf(
            PoseImageStoreOutcome.Stored(11L),
            PoseImageStoreOutcome.Stored(12L),
        )
        var calls = 0
        every { poseRepository.saveAll(any()) } answers {
            calls++
            if (calls == 1) throw RuntimeException("db down")
            firstArg<List<Pose>>().map {
                Pose(id = 2000L, mediaId = it.mediaId, headCount = it.headCount, sourceKey = it.sourceKey)
            }
        }
        every { mediaClient.discardPoseImage(1L, 11L) } throws RuntimeException("s3 down")

        // When
        val result = useCase.execute(cmd)

        // Then
        result.failed shouldBe listOf(PoseResult.BulkUploadPoses.Failed("a", 2, BulkFailReason.SAVE_FAILED))
        result.created.map { it.sourceKey } shouldBe listOf("b")
    }

    @Test
    @DisplayName("재조회가 예외를 던지면 SAVE_FAILED 로 보고하고 계속한다")
    fun recheckFailureIsSaveFailed() {
        // Given
        val cmd = command(rows = listOf(row(2, "a")), images = listOf(image("a.png")))
        var lookups = 0
        every { poseRepository.findExistingSourceKeys(any()) } answers {
            lookups++
            if (lookups == 1) emptySet() else throw RuntimeException("db down")
        }
        every { mediaClient.storePoseImage(1L, any()) } returns PoseImageStoreOutcome.Stored(11L)
        every { poseRepository.saveAll(any()) } throws RuntimeException("db down")
        every { mediaClient.discardPoseImage(1L, 11L) } just Runs

        // When
        val result = useCase.execute(cmd)

        // Then
        result.failed shouldBe listOf(PoseResult.BulkUploadPoses.Failed("a", 2, BulkFailReason.SAVE_FAILED))
    }

    @Test
    @DisplayName("계획 단계의 skipped/failed/unmatched 를 그대로 보고하고 이미지 저장을 시도하지 않는다")
    fun planOutcomesAreReported() {
        // Given
        every { poseRepository.findExistingSourceKeys(any()) } returns setOf("done")
        val cmd = command(
            rows = listOf(row(2, "done"), row(3, "couple_001", tag = "couple"), row(4, "bad", "ONE", "friend")),
            images = listOf(image("done.png"), image("bad.png"), image("stray.png")),
        )

        // When
        val result = useCase.execute(cmd)

        // Then
        result.skipped shouldBe listOf(
            PoseResult.BulkUploadPoses.Skipped("done", 2, BulkSkipReason.ALREADY_REGISTERED),
            PoseResult.BulkUploadPoses.Skipped("couple_001", 3, BulkSkipReason.IMAGE_NOT_PROVIDED),
        )
        result.failed shouldBe listOf(PoseResult.BulkUploadPoses.Failed("bad", 4, BulkFailReason.RELATION_NOT_ALLOWED))
        result.unmatchedImages shouldBe listOf("stray.png")
        verify(exactly = 0) { mediaClient.storePoseImage(any(), any()) }
    }
}
