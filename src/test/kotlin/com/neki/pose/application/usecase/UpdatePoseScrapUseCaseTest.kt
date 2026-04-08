package com.neki.pose.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.command.UpdatePoseScrapCommand
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.ScrapPoseRepositoryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdatePoseScrapUseCaseTest {

    private lateinit var poseRepository: PoseRepositoryPort
    private lateinit var scrapPoseRepository: ScrapPoseRepositoryPort
    private lateinit var useCase: UpdatePoseScrapUseCase

    @BeforeEach
    fun setUp() {
        poseRepository = mockk()
        scrapPoseRepository = mockk()
        useCase = UpdatePoseScrapUseCase(poseRepository, scrapPoseRepository)
    }

    @Test
    @DisplayName("스크랩 추가 (scrap=true) - add() 호출 확인")
    fun `스크랩 추가 (scrap=true) - add() 호출 확인`() {
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

    @Test
    @DisplayName("스크랩 해제 (scrap=false) - delete() 호출 확인")
    fun `스크랩 해제 (scrap=false) - delete() 호출 확인`() {
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

    @Test
    @DisplayName("포즈 미존재 → BusinessException(NOT_FOUND)")
    fun `포즈 미존재 → BusinessException(NOT_FOUND)`() {
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

    @Test
    @DisplayName("이미 스크랩된 포즈에 add - 멱등성 (예외 없이 add 호출)")
    fun `이미 스크랩된 포즈에 add - 멱등성 (예외 없이 add 호출)`() {
        // Given
        val command = UpdatePoseScrapCommand(userId = 1L, poseId = 10L, scrap = true)
        every { poseRepository.existsPose(10L) } returns true
        every { scrapPoseRepository.add(1L, 10L) } just Runs

        // When
        useCase.execute(command)

        // Then - 예외 없이 정상 처리
        verify(exactly = 1) { scrapPoseRepository.add(1L, 10L) }
    }

    @Test
    @DisplayName("스크랩 안 된 포즈에 delete - 멱등성 (예외 없이 delete 호출)")
    fun `스크랩 안 된 포즈에 delete - 멱등성 (예외 없이 delete 호출)`() {
        // Given
        val command = UpdatePoseScrapCommand(userId = 1L, poseId = 10L, scrap = false)
        every { poseRepository.existsPose(10L) } returns true
        every { scrapPoseRepository.delete(1L, 10L) } just Runs

        // When
        useCase.execute(command)

        // Then - 예외 없이 정상 처리
        verify(exactly = 1) { scrapPoseRepository.delete(1L, 10L) }
    }
}
