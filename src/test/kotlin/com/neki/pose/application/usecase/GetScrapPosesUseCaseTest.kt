package com.neki.pose.application.usecase

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.application.command.GetScrapPosesCommand
import com.neki.pose.application.contract.MediaStorageInfo
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.domain.entity.Pose
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aPose
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class GetScrapPosesUseCaseTest :
    FunSpec({

        lateinit var poseRepository: PoseRepositoryPort
        lateinit var mediaClient: MediaClientPort
        lateinit var transactionRunner: FakeTransactionRunner
        lateinit var useCase: GetScrapPosesUseCase

        beforeTest {
            poseRepository = mockk()
            mediaClient = mockk()
            transactionRunner = FakeTransactionRunner()
            useCase = GetScrapPosesUseCase(poseRepository, mediaClient, transactionRunner)
        }

        fun makeCommand(page: Int = 0, size: Int = 10): GetScrapPosesCommand = GetScrapPosesCommand(
            userId = 1L,
            page = page,
            size = size,
            headCount = null,
            sortOrder = SortOrder.DESC,
        )

        fun makePose(id: Long, mediaId: Long): Pose {
            val pose = aPose(id = id, mediaId = mediaId)
            pose.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
            return pose
        }

        fun makeMediaStorageInfo(mediaId: Long): MediaStorageInfo = MediaStorageInfo(
            mediaId = mediaId,
            storageKey = "pose/image-$mediaId.jpg",
            contentType = "image/jpeg",
            width = 800,
            height = 600,
        )

        test("정상 조회 + 미디어 매핑 - poses + storageInfo 반환") {
            // Given
            val command = makeCommand(size = 2)
            val poseList = listOf(makePose(1L, 101L), makePose(2L, 102L))

            every {
                poseRepository.listOwnedScrapPoses(
                    userId = 1L,
                    offset = 0,
                    limit = 3,
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
            result.poses[0].scrap shouldBe true
            result.poses[0].storageKey shouldBe "pose/image-101.jpg"
            result.poses[1].poseId shouldBe 2L
        }

        test("hasNext=true - 다음 페이지 존재") {
            // Given
            val command = makeCommand(size = 2)
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

        test("hasNext=false - 마지막 페이지") {
            // Given
            val command = makeCommand(size = 5)
            val poseList = listOf(makePose(1L, 101L), makePose(2L, 102L))

            every {
                poseRepository.listOwnedScrapPoses(
                    userId = 1L,
                    offset = 0,
                    limit = 6,
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
            result.hasNext shouldBe false
            result.poses.size shouldBe 2
        }

        test("빈 결과 - 빈 리스트와 hasNext=false 반환") {
            // Given
            val command = makeCommand()
            every {
                poseRepository.listOwnedScrapPoses(
                    userId = 1L,
                    offset = 0,
                    limit = 11,
                    sortOrder = SortOrder.DESC,
                )
            } returns emptyList()

            // When
            val result = useCase.execute(command)

            // Then
            result.poses shouldBe emptyList()
            result.hasNext shouldBe false
        }
    })
