package com.neki.api.pose.application.usecase

import com.neki.api.pose.application.UploadPosesUseCase
import com.neki.api.testfixture.FakeTransactionRunner
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.MediaAvailability
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.pose.repository.PoseRepository
import com.neki.domain.pose.service.PoseService
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

class UploadPosesUseCaseTest {

    private lateinit var mediaClient: MediaClient
    private lateinit var poseRepository: PoseRepository
    private lateinit var transactionRunner: FakeTransactionRunner
    private lateinit var useCase: UploadPosesUseCase

    @BeforeEach
    fun setUp() {
        mediaClient = mockk()
        poseRepository = mockk()
        transactionRunner = FakeTransactionRunner()
        // 빈 입력 처리는 저장소(PosesQueryRepository)가 맡으므로 서비스는 항상 포트를 호출한다
        every { poseRepository.findExistingSourceKeys(any()) } returns emptySet()
        useCase = UploadPosesUseCase(
            PoseService(poseRepository),
            mediaClient,
            transactionRunner,
        )
    }

    private fun makeUploadItem(mediaId: Long, headCount: HeadCount = HeadCount.TWO): PoseCommand.UploadPoses.Item =
        PoseCommand.UploadPoses.Item(mediaId = mediaId, headCount = headCount, memo = null)

    private fun makeCommand(userId: Long = 1L, uploads: List<PoseCommand.UploadPoses.Item>): PoseCommand.UploadPoses =
        PoseCommand.UploadPoses(userId = userId, uploads = uploads)

    @Test
    @DisplayName("정상 업로드 - 미디어 검증 → pose 저장")
    fun `정상 업로드 - 미디어 검증 → pose 저장`() {
        // Given
        val uploads = listOf(makeUploadItem(101L), makeUploadItem(102L))
        val command = makeCommand(uploads = uploads)

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L, 102L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE, 102L to MediaAvailability.AVAILABLE)

        every { poseRepository.saveAll(any()) } answers { firstArg() }

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { poseRepository.saveAll(any()) }
        verify(exactly = 0) { mediaClient.rollbackMediasUploaded(any(), any()) }
    }

    @Test
    @DisplayName("중복 mediaId → BusinessException(INVALID_PARAMETER)")
    fun `중복 mediaId → BusinessException(INVALID_PARAMETER)`() {
        // Given: 같은 mediaId가 두 번
        val uploads = listOf(makeUploadItem(101L), makeUploadItem(101L))
        val command = makeCommand(uploads = uploads)

        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        verify(exactly = 0) { mediaClient.verifyMediasUploaded(any(), any()) }
    }

    @Test
    @DisplayName("일부 미디어 UNAVAILABLE - 사용 가능한 미디어 롤백 후 BusinessException(UPLOAD_FAILED)")
    fun `일부 미디어 UNAVAILABLE - 사용 가능한 미디어 롤백 후 BusinessException(UPLOAD_FAILED)`() {
        // Given
        val uploads = listOf(makeUploadItem(101L), makeUploadItem(102L), makeUploadItem(103L))
        val command = makeCommand(uploads = uploads)

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L, 102L, 103L))
        } returns mapOf(
            101L to MediaAvailability.AVAILABLE,
            102L to MediaAvailability.UNAVAILABLE,
            103L to MediaAvailability.AVAILABLE,
        )
        every { mediaClient.rollbackMediasUploaded(1L, any()) } just Runs

        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.UPLOAD_FAILED
        // 성공한 미디어 101, 103 롤백 확인
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(eq(1L), any()) }
        verify(exactly = 0) { poseRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("트랜잭션 실패 → 롤백 - 미디어 rollback 호출 확인")
    fun `트랜잭션 실패 → 롤백 - 미디어 rollback 호출 확인`() {
        // Given
        val uploads = listOf(makeUploadItem(101L), makeUploadItem(102L))
        val command = makeCommand(uploads = uploads)

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L, 102L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE, 102L to MediaAvailability.AVAILABLE)

        every { poseRepository.saveAll(any()) } throws RuntimeException("DB 저장 실패")
        every { mediaClient.rollbackMediasUploaded(1L, listOf(101L, 102L)) } just Runs

        // When / Then
        shouldThrow<RuntimeException> {
            useCase.execute(command)
        }
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(1L, listOf(101L, 102L)) }
    }

    @Test
    @DisplayName("롤백 중 예외 발생 - 롤백 예외가 전파됨")
    fun `롤백 중 예외 발생 - 롤백 예외가 전파됨`() {
        // Given
        val uploads = listOf(makeUploadItem(101L))
        val command = makeCommand(uploads = uploads)
        val originalException = RuntimeException("원래 예외")
        val rollbackException = RuntimeException("롤백 실패")

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE)

        every { poseRepository.saveAll(any()) } throws originalException
        every { mediaClient.rollbackMediasUploaded(1L, listOf(101L)) } throws rollbackException

        // When / Then
        // rollback 중 예외가 발생하면 rollback 예외가 전파됨 (원래 예외는 catch 블록 안에서 소실)
        val ex = shouldThrow<RuntimeException> {
            useCase.execute(command)
        }
        ex.message shouldBe "롤백 실패"
    }

    @Test
    @DisplayName("빈 uploads 목록 - 저장 0건 정상 처리")
    fun `빈 uploads 목록 - 저장 0건 정상 처리`() {
        // Given
        val command = makeCommand(uploads = emptyList())

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = emptyList())
        } returns emptyMap()

        every { poseRepository.saveAll(emptyList()) } returns emptyList()

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { poseRepository.saveAll(emptyList()) }
        verify(exactly = 0) { mediaClient.rollbackMediasUploaded(any(), any()) }
    }

    @Test
    @DisplayName("relation + TWO - relation 이 포즈에 저장된다")
    fun `relation + TWO - relation 이 포즈에 저장된다`() {
        // Given
        val uploads = listOf(
            PoseCommand.UploadPoses.Item(
                mediaId = 101L,
                headCount = HeadCount.TWO,
                memo = null,
                relation = PoseRelation.COUPLE,
            ),
        )
        val command = makeCommand(uploads = uploads)
        val saved = slot<List<Pose>>()

        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE)
        every { poseRepository.saveAll(capture(saved)) } answers { firstArg() }

        // When
        useCase.execute(command)

        // Then
        saved.captured.single().relation shouldBe PoseRelation.COUPLE
        saved.captured.single().sourceKey shouldBe null
    }

    @Test
    @DisplayName("relation + ONE - 요청 전체가 INVALID_PARAMETER, 저장하지 않는다")
    fun `relation + ONE - 요청 전체가 INVALID_PARAMETER, 저장하지 않는다`() {
        // Given: 정상 행과 관계가 붙은 1인 행이 섞여 있다
        val uploads = listOf(
            makeUploadItem(101L),
            PoseCommand.UploadPoses.Item(
                mediaId = 102L,
                headCount = HeadCount.ONE,
                memo = null,
                relation = PoseRelation.FRIEND,
            ),
        )
        val command = makeCommand(uploads = uploads)

        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        verify(exactly = 0) { mediaClient.verifyMediasUploaded(any(), any()) }
        verify(exactly = 0) { poseRepository.saveAll(any()) }
    }

    private fun keyed(mediaId: Long, sourceKey: String?): PoseCommand.UploadPoses.Item = PoseCommand.UploadPoses.Item(
        mediaId = mediaId,
        headCount = HeadCount.TWO,
        memo = null,
        relation = PoseRelation.FRIEND,
        sourceKey = sourceKey,
    )

    @Test
    @DisplayName("sourceKey + relation - 포즈에 sourceKey 가 들어간다")
    fun `sourceKey + relation - 포즈에 sourceKey 가 들어간다`() {
        // Given
        val command = makeCommand(uploads = listOf(keyed(101L, "friend_001"), keyed(102L, null)))
        val saved = slot<List<Pose>>()
        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L, 102L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE, 102L to MediaAvailability.AVAILABLE)
        every { poseRepository.saveAll(capture(saved)) } answers { firstArg() }

        // When
        useCase.execute(command)

        // Then
        saved.captured.map { it.sourceKey } shouldBe listOf("friend_001", null)
        saved.captured.map { it.relation } shouldBe listOf(PoseRelation.FRIEND, PoseRelation.FRIEND)
        verify(exactly = 1) { poseRepository.findExistingSourceKeys(listOf("friend_001")) }
    }

    @Test
    @DisplayName("요청 안 sourceKey 중복 - INVALID_PARAMETER, 사전 조회·verify·저장 없음")
    fun `요청 안 sourceKey 중복 - INVALID_PARAMETER, 사전 조회·verify·저장 없음`() {
        // Given
        val command = makeCommand(uploads = listOf(keyed(101L, "k1"), keyed(102L, "k1")))

        // When / Then
        val ex = shouldThrow<BusinessException> { useCase.execute(command) }
        ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        verify(exactly = 0) { poseRepository.findExistingSourceKeys(any()) }
        verify(exactly = 0) { mediaClient.verifyMediasUploaded(any(), any()) }
        verify(exactly = 0) { poseRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("이미 등록된 sourceKey - CONFLICT_POSE_SOURCE_KEY, verify·저장·롤백 없음 (media 상태 변화 없음)")
    fun `이미 등록된 sourceKey - CONFLICT_POSE_SOURCE_KEY, verify·저장·롤백 없음`() {
        // Given
        val command = makeCommand(uploads = listOf(keyed(101L, "k1"), keyed(102L, "k2")))
        every { poseRepository.findExistingSourceKeys(listOf("k1", "k2")) } returns setOf("k1")

        // When / Then
        val ex = shouldThrow<BusinessException> { useCase.execute(command) }
        ex.resultCode shouldBe ResultCode.CONFLICT_POSE_SOURCE_KEY
        verify(exactly = 0) { mediaClient.verifyMediasUploaded(any(), any()) }
        verify(exactly = 0) { poseRepository.saveAll(any()) }
        verify(exactly = 0) { mediaClient.rollbackMediasUploaded(any(), any()) }
    }

    @Test
    @DisplayName("저장 경합 - 저장 실패 후 재조회에 키가 있으면 롤백하고 CONFLICT_POSE_SOURCE_KEY")
    fun `저장 경합 - 저장 실패 후 재조회에 키가 있으면 롤백하고 CONFLICT_POSE_SOURCE_KEY`() {
        // Given
        val command = makeCommand(uploads = listOf(keyed(101L, "k1")))
        every { poseRepository.findExistingSourceKeys(listOf("k1")) } returnsMany listOf(emptySet(), setOf("k1"))
        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE)
        every { poseRepository.saveAll(any()) } throws RuntimeException("unique violation")
        every { mediaClient.rollbackMediasUploaded(1L, listOf(101L)) } just Runs

        // When / Then
        val ex = shouldThrow<BusinessException> { useCase.execute(command) }
        ex.resultCode shouldBe ResultCode.CONFLICT_POSE_SOURCE_KEY
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(1L, listOf(101L)) }
    }

    @Test
    @DisplayName("저장 실패, 재조회 결과 없음 - 롤백 후 원래 예외 전파")
    fun `저장 실패, 재조회 결과 없음 - 롤백 후 원래 예외 전파`() {
        // Given
        val command = makeCommand(uploads = listOf(keyed(101L, "k1")))
        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE)
        every { poseRepository.saveAll(any()) } throws IllegalStateException("db down")
        every { mediaClient.rollbackMediasUploaded(1L, listOf(101L)) } just Runs

        // When / Then
        shouldThrow<IllegalStateException> { useCase.execute(command) }.message shouldBe "db down"
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(1L, listOf(101L)) }
        verify(exactly = 2) { poseRepository.findExistingSourceKeys(listOf("k1")) }
    }

    @Test
    @DisplayName("저장 실패, 재조회도 실패 - 롤백 후 원래 예외 전파")
    fun `저장 실패, 재조회도 실패 - 롤백 후 원래 예외 전파`() {
        // Given
        val command = makeCommand(uploads = listOf(keyed(101L, "k1")))
        var lookups = 0
        every { poseRepository.findExistingSourceKeys(listOf("k1")) } answers {
            lookups++
            if (lookups == 1) emptySet() else throw RuntimeException("lookup failed")
        }
        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE)
        every { poseRepository.saveAll(any()) } throws IllegalStateException("db down")
        every { mediaClient.rollbackMediasUploaded(1L, listOf(101L)) } just Runs

        // When / Then
        shouldThrow<IllegalStateException> { useCase.execute(command) }.message shouldBe "db down"
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(1L, listOf(101L)) }
    }

    @Test
    @DisplayName("sourceKey 가 없으면 사전 조회에 빈 목록이 간다 (실제 쿼리 생략은 PosesQueryRepository 가 맡는다)")
    fun `sourceKey 가 없으면 사전 조회에 빈 목록이 간다`() {
        // Given
        val command = makeCommand(uploads = listOf(makeUploadItem(101L)))
        every {
            mediaClient.verifyMediasUploaded(ownerId = 1L, mediaIds = listOf(101L))
        } returns mapOf(101L to MediaAvailability.AVAILABLE)
        every { poseRepository.saveAll(any()) } answers { firstArg() }

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { poseRepository.findExistingSourceKeys(emptyList()) }
    }
}
