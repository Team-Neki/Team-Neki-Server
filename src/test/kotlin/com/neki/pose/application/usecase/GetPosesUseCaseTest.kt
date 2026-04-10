package com.neki.pose.application.usecase

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.application.command.GetPosesCommand
import com.neki.pose.application.contract.MediaStorageInfo
import com.neki.pose.application.contract.PoseWithScrap
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aPose
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GetPosesUseCaseTest {

    private lateinit var poseRepository: PoseRepositoryPort
    private lateinit var mediaClient: MediaClientPort
    private lateinit var transactionRunner: FakeTransactionRunner
    private lateinit var useCase: GetPosesUseCase

    @BeforeEach
    fun setUp() {
        poseRepository = mockk()
        mediaClient = mockk()
        transactionRunner = FakeTransactionRunner()
        useCase = GetPosesUseCase(poseRepository, mediaClient, transactionRunner)
    }

    private fun makeCommand(page: Int = 0, size: Int = 10): GetPosesCommand = GetPosesCommand(
        userId = 1L,
        page = page,
        size = size,
        headCount = null,
        sortOrder = SortOrder.DESC,
    )

    private fun makePoseWithScrap(id: Long, mediaId: Long, scrapped: Boolean = false): PoseWithScrap {
        val pose = aPose(id = id, mediaId = mediaId)
        pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        return PoseWithScrap(pose = pose, isScraped = scrapped)
    }

    private fun makeMediaStorageInfo(mediaId: Long): MediaStorageInfo = MediaStorageInfo(
        mediaId = mediaId,
        storageKey = "pose/image-$mediaId.jpg",
        contentType = "image/jpeg",
        width = 800,
        height = 600,
    )

    @Test
    @DisplayName("정상 조회 + 미디어 매핑 - poses + storageInfo 반환")
    fun `정상 조회 + 미디어 매핑 - poses + storageInfo 반환`() {
        // Given
        val command = makeCommand(size = 2)
        val poseList = listOf(
            makePoseWithScrap(id = 1L, mediaId = 101L, scrapped = false),
            makePoseWithScrap(id = 2L, mediaId = 102L, scrapped = true),
        )
        every {
            poseRepository.listPosesWithScrap(
                userId = 1L,
                offset = 0,
                limit = 3,
                headCount = null,
                sortOrder = SortOrder.DESC,
            )
        } returns poseList

        every { mediaClient.getMediaStorageInfos(listOf(101L, 102L)) } returns listOf(
            makeMediaStorageInfo(101L),
            makeMediaStorageInfo(102L),
        )

        // When
        val result = useCase.execute(command)

        // Then
        result.poses.size shouldBe 2
        result.hasNext shouldBe false
        result.poses[0].poseId shouldBe 1L
        result.poses[0].storageKey shouldBe "pose/image-101.jpg"
        result.poses[0].scrap shouldBe false
        result.poses[1].poseId shouldBe 2L
        result.poses[1].scrap shouldBe true
    }

    @Test
    @DisplayName("hasNext=true - 다음 페이지 존재")
    fun `hasNext=true - 다음 페이지 존재`() {
        // Given
        val command = makeCommand(size = 2)
        // size+1 = 3개 조회됨 → hasNext true
        val poseList = listOf(
            makePoseWithScrap(id = 1L, mediaId = 101L),
            makePoseWithScrap(id = 2L, mediaId = 102L),
            makePoseWithScrap(id = 3L, mediaId = 103L),
        )
        every {
            poseRepository.listPosesWithScrap(
                userId = 1L,
                offset = 0,
                limit = 3,
                headCount = null,
                sortOrder = SortOrder.DESC,
            )
        } returns poseList

        every { mediaClient.getMediaStorageInfos(listOf(101L, 102L)) } returns listOf(
            makeMediaStorageInfo(101L),
            makeMediaStorageInfo(102L),
        )

        // When
        val result = useCase.execute(command)

        // Then
        result.hasNext shouldBe true
        result.poses.size shouldBe 2
    }

    @Test
    @DisplayName("hasNext=false - 마지막 페이지")
    fun `hasNext=false - 마지막 페이지`() {
        // Given
        val command = makeCommand(size = 5)
        // size+1 = 6개 조회하려 했지만 3개만 있음 → hasNext false
        val poseList = listOf(
            makePoseWithScrap(id = 1L, mediaId = 101L),
            makePoseWithScrap(id = 2L, mediaId = 102L),
            makePoseWithScrap(id = 3L, mediaId = 103L),
        )
        every {
            poseRepository.listPosesWithScrap(
                userId = 1L,
                offset = 0,
                limit = 6,
                headCount = null,
                sortOrder = SortOrder.DESC,
            )
        } returns poseList

        every { mediaClient.getMediaStorageInfos(listOf(101L, 102L, 103L)) } returns listOf(
            makeMediaStorageInfo(101L),
            makeMediaStorageInfo(102L),
            makeMediaStorageInfo(103L),
        )

        // When
        val result = useCase.execute(command)

        // Then
        result.hasNext shouldBe false
        result.poses.size shouldBe 3
    }

    @Test
    @DisplayName("빈 결과 - 빈 리스트와 hasNext=false 반환")
    fun `빈 결과 - 빈 리스트와 hasNext=false 반환`() {
        // Given
        val command = makeCommand()
        every {
            poseRepository.listPosesWithScrap(
                userId = 1L,
                offset = 0,
                limit = 11,
                headCount = null,
                sortOrder = SortOrder.DESC,
            )
        } returns emptyList()

        // When
        val result = useCase.execute(command)

        // Then
        result.poses shouldBe emptyList()
        result.hasNext shouldBe false
    }

    @Test
    @DisplayName("일부 미디어 미존재 - mapNotNull로 필터링하여 일부만 반환")
    fun `일부 미디어 미존재 - mapNotNull로 필터링하여 일부만 반환`() {
        // Given
        val command = makeCommand(size = 3)
        val poseList = listOf(
            makePoseWithScrap(id = 1L, mediaId = 101L),
            makePoseWithScrap(id = 2L, mediaId = 102L),
            makePoseWithScrap(id = 3L, mediaId = 103L),
        )
        every {
            poseRepository.listPosesWithScrap(
                userId = 1L,
                offset = 0,
                limit = 4,
                headCount = null,
                sortOrder = SortOrder.DESC,
            )
        } returns poseList

        // mediaId=102L은 미존재
        every { mediaClient.getMediaStorageInfos(listOf(101L, 102L, 103L)) } returns listOf(
            makeMediaStorageInfo(101L),
            makeMediaStorageInfo(103L),
        )

        // When
        val result = useCase.execute(command)

        // Then
        result.poses.size shouldBe 2
        result.poses.map { it.poseId } shouldBe listOf(1L, 3L)
    }

    @Test
    @DisplayName("전체 미디어 미존재 - 빈 리스트 반환")
    fun `전체 미디어 미존재 - 빈 리스트 반환`() {
        // Given
        val command = makeCommand(size = 2)
        val poseList = listOf(
            makePoseWithScrap(id = 1L, mediaId = 101L),
            makePoseWithScrap(id = 2L, mediaId = 102L),
        )
        every {
            poseRepository.listPosesWithScrap(
                userId = 1L,
                offset = 0,
                limit = 3,
                headCount = null,
                sortOrder = SortOrder.DESC,
            )
        } returns poseList

        every { mediaClient.getMediaStorageInfos(listOf(101L, 102L)) } returns emptyList()

        // When
        val result = useCase.execute(command)

        // Then
        result.poses shouldBe emptyList()
        result.hasNext shouldBe false
    }
}
