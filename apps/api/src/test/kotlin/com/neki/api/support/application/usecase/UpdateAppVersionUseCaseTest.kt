package com.neki.api.support.application.usecase

import com.neki.api.support.application.UpdateAppVersionUseCase
import com.neki.api.testfixture.anAppVersion
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.support.dto.AppVersionCommand
import com.neki.domain.support.models.AppVersion
import com.neki.domain.support.models.Platform
import com.neki.domain.support.repository.AppVersionRepository
import com.neki.domain.support.service.AppVersionService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateAppVersionUseCaseTest {

    private lateinit var appVersionRepository: AppVersionRepository
    private lateinit var useCase: UpdateAppVersionUseCase

    @BeforeEach
    fun setUp() {
        appVersionRepository = mockk()
        useCase = UpdateAppVersionUseCase(AppVersionService(appVersionRepository))
    }

    @Test
    @DisplayName("정상 업데이트 - platform을 찾아 버전을 업데이트하고 save를 호출한다")
    fun `정상 업데이트 - platform을 찾아 버전을 업데이트하고 save를 호출한다`() {
        // Given
        val platform = Platform.IOS
        val appVersion = anAppVersion(platform = platform, minVersion = "1.0.0", currentVersion = "1.2.0")
        val savedSlot = slot<AppVersion>()

        every { appVersionRepository.findByPlatform(platform) } returns appVersion
        every { appVersionRepository.save(capture(savedSlot)) } returns appVersion

        // When
        useCase.execute(
            AppVersionCommand.UpdateAppVersion(platform = platform, minVersion = "1.1.0", currentVersion = "2.0.0"),
        )

        // Then
        verify(exactly = 1) { appVersionRepository.save(any()) }
        savedSlot.captured.minVersion shouldBe "1.1.0"
        savedSlot.captured.currentVersion shouldBe "2.0.0"
    }

    @Test
    @DisplayName("존재하지 않는 platform - BusinessException(NOT_FOUND)을 던진다")
    fun `존재하지 않는 platform - BusinessException(NOT_FOUND)을 던진다`() {
        // Given
        val platform = Platform.ANDROID
        every { appVersionRepository.findByPlatform(platform) } returns null

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(
                AppVersionCommand.UpdateAppVersion(platform = platform, minVersion = "1.0.0", currentVersion = "2.0.0"),
            )
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { appVersionRepository.save(any()) }
    }
}
