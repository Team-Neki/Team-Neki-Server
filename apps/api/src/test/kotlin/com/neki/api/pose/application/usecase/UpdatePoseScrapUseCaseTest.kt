package com.neki.api.pose.application.usecase

import com.neki.api.pose.application.UpdatePoseScrapUseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.models.ScrapPose
import com.neki.domain.pose.repository.PoseRepository
import com.neki.domain.pose.repository.ScrapPoseRepository
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

class UpdatePoseScrapUseCaseTest {

    private lateinit var poseRepository: PoseRepository
    private lateinit var scrapPoseRepository: ScrapPoseRepository
    private lateinit var useCase: UpdatePoseScrapUseCase

    @BeforeEach
    fun setUp() {
        poseRepository = mockk()
        scrapPoseRepository = mockk()
        useCase = UpdatePoseScrapUseCase(PoseService(poseRepository, scrapPoseRepository, mockk(), mockk()))
    }

    @Test
    @DisplayName("스크랩 추가 (scrap=true) - add() 호출 확인")
    fun `스크랩 추가 (scrap=true) - add() 호출 확인`() {
        // Given
        val command = PoseCommand.UpdatePoseScrap(userId = 1L, poseId = 10L, scrap = true)
        val scrapPoseSlot = slot<ScrapPose>()
        every { poseRepository.existsPose(10L) } returns true
        every { scrapPoseRepository.add(capture(scrapPoseSlot)) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { scrapPoseRepository.add(any()) }
        verify(exactly = 0) { scrapPoseRepository.delete(any()) }
        scrapPoseSlot.captured.id.userId shouldBe 1L
        scrapPoseSlot.captured.id.poseId shouldBe 10L
    }

    @Test
    @DisplayName("스크랩 해제 (scrap=false) - delete() 호출 확인")
    fun `스크랩 해제 (scrap=false) - delete() 호출 확인`() {
        // Given
        val command = PoseCommand.UpdatePoseScrap(userId = 1L, poseId = 10L, scrap = false)
        val scrapPoseSlot = slot<ScrapPose>()
        every { poseRepository.existsPose(10L) } returns true
        every { scrapPoseRepository.delete(capture(scrapPoseSlot)) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { scrapPoseRepository.delete(any()) }
        verify(exactly = 0) { scrapPoseRepository.add(any()) }
        scrapPoseSlot.captured.id.userId shouldBe 1L
        scrapPoseSlot.captured.id.poseId shouldBe 10L
    }

    @Test
    @DisplayName("포즈 미존재 → BusinessException(NOT_FOUND)")
    fun `포즈 미존재 → BusinessException(NOT_FOUND)`() {
        // Given
        val command = PoseCommand.UpdatePoseScrap(userId = 1L, poseId = 999L, scrap = true)
        every { poseRepository.existsPose(999L) } returns false

        // When / Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { scrapPoseRepository.add(any()) }
        verify(exactly = 0) { scrapPoseRepository.delete(any()) }
    }

    @Test
    @DisplayName("이미 스크랩된 포즈에 add - 멱등성 (예외 없이 add 호출)")
    fun `이미 스크랩된 포즈에 add - 멱등성 (예외 없이 add 호출)`() {
        // Given
        val command = PoseCommand.UpdatePoseScrap(userId = 1L, poseId = 10L, scrap = true)
        every { poseRepository.existsPose(10L) } returns true
        every { scrapPoseRepository.add(any()) } just Runs

        // When
        useCase.execute(command)

        // Then - 예외 없이 정상 처리
        verify(exactly = 1) { scrapPoseRepository.add(any()) }
    }

    @Test
    @DisplayName("스크랩 안 된 포즈에 delete - 멱등성 (예외 없이 delete 호출)")
    fun `스크랩 안 된 포즈에 delete - 멱등성 (예외 없이 delete 호출)`() {
        // Given
        val command = PoseCommand.UpdatePoseScrap(userId = 1L, poseId = 10L, scrap = false)
        every { poseRepository.existsPose(10L) } returns true
        every { scrapPoseRepository.delete(any()) } just Runs

        // When
        useCase.execute(command)

        // Then - 예외 없이 정상 처리
        verify(exactly = 1) { scrapPoseRepository.delete(any()) }
    }
}
