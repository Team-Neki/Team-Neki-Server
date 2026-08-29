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

/**
 * fileName       : PoseServiceTest
 * description    : PoseService 이미지 교체 단위 테스트
 */
class PoseServiceTest :
    FunSpec({

        val poseRepository = mockk<PoseRepository>()
        val poseService = PoseService(poseRepository)

        test("updatePoseMedia - 조회한 포즈의 이미지를 교체한다") {
            // Given
            val pose = Pose(id = 1L, mediaId = 10L, headCount = HeadCount.ONE)
            every { poseRepository.findById(1L) } returns pose

            // When
            val updated = poseService.updatePoseMedia(PoseCommand.UpdatePoseMedia(poseId = 1L, mediaId = 20L))

            // Then
            updated.mediaId shouldBe 20L
        }

        test("updatePoseMedia - 존재하지 않는 포즈면 NOT_FOUND가 전파된다") {
            // Given
            every { poseRepository.findById(999L) } throws BusinessException(ResultCode.NOT_FOUND)

            // When
            val ex = shouldThrow<BusinessException> {
                poseService.updatePoseMedia(PoseCommand.UpdatePoseMedia(poseId = 999L, mediaId = 20L))
            }

            // Then
            ex.resultCode shouldBe ResultCode.NOT_FOUND
        }
    })
