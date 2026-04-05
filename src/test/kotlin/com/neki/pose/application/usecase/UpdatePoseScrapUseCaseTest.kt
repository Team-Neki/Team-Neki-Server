package com.neki.pose.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.command.UpdatePoseScrapCommand
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.ScrapPoseRepositoryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class UpdatePoseScrapUseCaseTest :
    FunSpec({

        lateinit var poseRepository: PoseRepositoryPort
        lateinit var scrapPoseRepository: ScrapPoseRepositoryPort
        lateinit var useCase: UpdatePoseScrapUseCase

        beforeTest {
            poseRepository = mockk()
            scrapPoseRepository = mockk()
            useCase = UpdatePoseScrapUseCase(poseRepository, scrapPoseRepository)
        }

        test("스크랩 추가 (scrap=true) - add() 호출 확인") {
            // Given
            val command = UpdatePoseScrapCommand(userId = 1L, poseId = 10L, scrap = true)
            every { poseRepository.existsPose(10L) } returns true
            every { scrapPoseRepository.add(1L, 10L) } just Runs

            // When
            useCase.execute(command)

            // Then
            verify(exactly = 1) { scrapPoseRepository.add(1L, 10L) }
            verify(exactly = 0) { scrapPoseRepository.delete(any(), any()) }
        }

        test("스크랩 해제 (scrap=false) - delete() 호출 확인") {
            // Given
            val command = UpdatePoseScrapCommand(userId = 1L, poseId = 10L, scrap = false)
            every { poseRepository.existsPose(10L) } returns true
            every { scrapPoseRepository.delete(1L, 10L) } just Runs

            // When
            useCase.execute(command)

            // Then
            verify(exactly = 1) { scrapPoseRepository.delete(1L, 10L) }
            verify(exactly = 0) { scrapPoseRepository.add(any(), any()) }
        }

        test("포즈 미존재 → BusinessException(NOT_FOUND)") {
            // Given
            val command = UpdatePoseScrapCommand(userId = 1L, poseId = 999L, scrap = true)
            every { poseRepository.existsPose(999L) } returns false

            // When / Then
            val ex = shouldThrow<BusinessException> {
                useCase.execute(command)
            }
            ex.resultCode shouldBe ResultCode.NOT_FOUND
            verify(exactly = 0) { scrapPoseRepository.add(any(), any()) }
            verify(exactly = 0) { scrapPoseRepository.delete(any(), any()) }
        }

        test("이미 스크랩된 포즈에 add - 멱등성 (예외 없이 add 호출)") {
            // Given
            val command = UpdatePoseScrapCommand(userId = 1L, poseId = 10L, scrap = true)
            every { poseRepository.existsPose(10L) } returns true
            every { scrapPoseRepository.add(1L, 10L) } just Runs

            // When
            useCase.execute(command)

            // Then - 예외 없이 정상 처리
            verify(exactly = 1) { scrapPoseRepository.add(1L, 10L) }
        }

        test("스크랩 안 된 포즈에 delete - 멱등성 (예외 없이 delete 호출)") {
            // Given
            val command = UpdatePoseScrapCommand(userId = 1L, poseId = 10L, scrap = false)
            every { poseRepository.existsPose(10L) } returns true
            every { scrapPoseRepository.delete(1L, 10L) } just Runs

            // When
            useCase.execute(command)

            // Then - 예외 없이 정상 처리
            verify(exactly = 1) { scrapPoseRepository.delete(1L, 10L) }
        }
    })
