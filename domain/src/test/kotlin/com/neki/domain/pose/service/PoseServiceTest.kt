package com.neki.domain.pose.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.repository.PoseRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * fileName       : PoseServiceTest
 * description    : PoseService 이미지 교체·media 사용 중 검사 단위 테스트
 */
class PoseServiceTest :
    FunSpec({

        val poseRepository = mockk<PoseRepository>()
        val poseService = PoseService(poseRepository)

        test("updatePoseMedia - 조회한 포즈의 이미지를 교체한다") {
            // Given
            val pose = Pose(id = 1L, mediaId = 10L, headCount = HeadCount.ONE)
            every { poseRepository.findById(1L) } returns pose
            every { poseRepository.existsByMediaIdIn(listOf(20L)) } returns false

            // When
            val updated = poseService.updatePoseMedia(PoseCommand.UpdatePoseMedia(poseId = 1L, mediaId = 20L))

            // Then
            updated.mediaId shouldBe 20L
        }

        test("updatePoseMedia - 존재하지 않는 포즈면 NOT_FOUND") {
            // Given
            every { poseRepository.findById(999L) } returns null

            // When
            val ex = shouldThrow<BusinessException> {
                poseService.updatePoseMedia(PoseCommand.UpdatePoseMedia(poseId = 999L, mediaId = 20L))
            }

            // Then
            ex.resultCode shouldBe ResultCode.NOT_FOUND
        }

        test("updatePoseMedia - 다른 포즈가 쓰는 media 로 바꾸면 INVALID_PARAMETER 이고 이미지가 그대로다") {
            // Given
            val pose = Pose(id = 2L, mediaId = 10L, headCount = HeadCount.ONE)
            every { poseRepository.findById(2L) } returns pose
            every { poseRepository.existsByMediaIdIn(listOf(30L)) } returns true

            // When
            val ex = shouldThrow<BusinessException> {
                poseService.updatePoseMedia(PoseCommand.UpdatePoseMedia(poseId = 2L, mediaId = 30L))
            }

            // Then
            ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
            pose.mediaId shouldBe 10L
        }

        test("updatePoseMedia - 지금 쓰는 media 를 그대로 보내면 사용 중 검사를 하지 않는다") {
            // Given
            val pose = Pose(id = 3L, mediaId = 10L, headCount = HeadCount.ONE)
            every { poseRepository.findById(3L) } returns pose

            // When
            val updated = poseService.updatePoseMedia(PoseCommand.UpdatePoseMedia(poseId = 3L, mediaId = 10L))

            // Then
            updated.mediaId shouldBe 10L
            verify(exactly = 0) { poseRepository.existsByMediaIdIn(listOf(10L)) }
        }

        test("validateMediaNotInUse - 이미 포즈에 연결된 media 가 있으면 INVALID_PARAMETER") {
            // Given
            every { poseRepository.existsByMediaIdIn(listOf(40L, 41L)) } returns true

            // When
            val ex = shouldThrow<BusinessException> { poseService.validateMediaNotInUse(listOf(40L, 41L)) }

            // Then
            ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        }
    })
