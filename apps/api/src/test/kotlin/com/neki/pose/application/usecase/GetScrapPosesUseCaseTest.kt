package com.neki.pose.application.usecase

import com.neki.common.domain.vo.Pagination
import com.neki.common.domain.vo.SortOrder
import com.neki.pose.application.GetScrapPosesUseCase
import com.neki.pose.client.MediaClient
import com.neki.pose.dto.PoseQuery
import com.neki.pose.models.MediaMetadata
import com.neki.pose.models.Pose
import com.neki.pose.repository.PoseRepository
import com.neki.pose.service.PoseService
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aPose
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GetScrapPosesUseCaseTest {

    private lateinit var poseRepository: PoseRepository
    private lateinit var mediaClient: MediaClient
    private lateinit var transactionRunner: FakeTransactionRunner
    private lateinit var useCase: GetScrapPosesUseCase

    @BeforeEach
    fun setUp() {
        poseRepository = mockk()
        mediaClient = mockk()
        transactionRunner = FakeTransactionRunner()
        useCase = GetScrapPosesUseCase(
            PoseService(poseRepository, mockk(), mockk(), mockk()),
            mediaClient,
            transactionRunner,
        )
    }

    private fun makeQuery(page: Int = 0, size: Int = 10): PoseQuery.GetScrapPoses = PoseQuery.GetScrapPoses(
        userId = 1L,
        headCount = null,
        pagination = Pagination(page = page, size = size, sortOrder = SortOrder.DESC),
    )

    private fun makePose(id: Long, mediaId: Long): Pose {
        val pose = aPose(id = id, mediaId = mediaId)
        pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        return pose
    }

    private fun makeMediaMetadata(mediaId: Long): MediaMetadata = MediaMetadata(
        mediaId = mediaId,
        storageKey = "pose/image-$mediaId.jpg",
        contentType = "image/jpeg",
        width = 800,
        height = 600,
    )

    @Test
    @DisplayName("정상 조회 + 미디어 매핑 - poses + metadata 반환")
    fun `정상 조회 + 미디어 매핑 - poses + metadata 반환`() {
        // Given
        val query = makeQuery(size = 2)
        val poseList = listOf(makePose(1L, 101L), makePose(2L, 102L))

        every {
            poseRepository.listOwnedScrapPoses(
                userId = 1L,
                offset = 0,
                limit = 3,
                sortOrder = SortOrder.DESC,
            )
        } returns poseList

        every { mediaClient.getMediaMetadata(listOf(101L, 102L)) } returns listOf(
            makeMediaMetadata(101L),
            makeMediaMetadata(102L),
        )

        // When
        val result = useCase.execute(query)

        // Then
        result.poses.size shouldBe 2
        result.hasNext shouldBe false
        result.poses[0].poseId shouldBe 1L
        result.poses[0].scrap shouldBe true
        result.poses[0].storageKey shouldBe "pose/image-101.jpg"
        result.poses[1].poseId shouldBe 2L
    }

    @Test
    @DisplayName("hasNext=true - 다음 페이지 존재")
    fun `hasNext=true - 다음 페이지 존재`() {
        // Given
        val query = makeQuery(size = 2)
        // size+1 = 3개 조회됨 → hasNext true
        val poseList = listOf(
            makePose(1L, 101L),
            makePose(2L, 102L),
            makePose(3L, 103L),
        )
        every {
            poseRepository.listOwnedScrapPoses(
                userId = 1L,
                offset = 0,
                limit = 3,
                sortOrder = SortOrder.DESC,
            )
        } returns poseList

        every { mediaClient.getMediaMetadata(listOf(101L, 102L)) } returns listOf(
            makeMediaMetadata(101L),
            makeMediaMetadata(102L),
        )

        // When
        val result = useCase.execute(query)

        // Then
        result.hasNext shouldBe true
        result.poses.size shouldBe 2
    }

    @Test
    @DisplayName("hasNext=false - 마지막 페이지")
    fun `hasNext=false - 마지막 페이지`() {
        // Given
        val query = makeQuery(size = 5)
        val poseList = listOf(makePose(1L, 101L), makePose(2L, 102L))

        every {
            poseRepository.listOwnedScrapPoses(
                userId = 1L,
                offset = 0,
                limit = 6,
                sortOrder = SortOrder.DESC,
            )
        } returns poseList

        every { mediaClient.getMediaMetadata(listOf(101L, 102L)) } returns listOf(
            makeMediaMetadata(101L),
            makeMediaMetadata(102L),
        )

        // When
        val result = useCase.execute(query)

        // Then
        result.hasNext shouldBe false
        result.poses.size shouldBe 2
    }

    @Test
    @DisplayName("빈 결과 - 빈 리스트와 hasNext=false 반환")
    fun `빈 결과 - 빈 리스트와 hasNext=false 반환`() {
        // Given
        val query = makeQuery()
        every {
            poseRepository.listOwnedScrapPoses(
                userId = 1L,
                offset = 0,
                limit = 11,
                sortOrder = SortOrder.DESC,
            )
        } returns emptyList()

        // When
        val result = useCase.execute(query)

        // Then
        result.poses shouldBe emptyList()
        result.hasNext shouldBe false
    }
}
