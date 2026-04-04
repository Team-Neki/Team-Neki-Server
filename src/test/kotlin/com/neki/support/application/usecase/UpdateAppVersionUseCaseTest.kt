package com.neki.support.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.application.command.UpdateAppVersionCommand
import com.neki.support.application.port.AppVersionRepositoryPort
import com.neki.support.domain.entity.AppVersion
import com.neki.support.domain.enums.Platform
import com.neki.testfixture.anAppVersion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class UpdateAppVersionUseCaseTest : FunSpec({

    lateinit var appVersionRepository: AppVersionRepositoryPort
    lateinit var useCase: UpdateAppVersionUseCase

    beforeTest {
        appVersionRepository = mockk()
        useCase = UpdateAppVersionUseCase(appVersionRepository)
    }

    test("정상 업데이트 - platform을 찾아 버전을 업데이트하고 save를 호출한다") {
        // Given
        val platform = Platform.IOS
        val appVersion = anAppVersion(platform = platform, minVersion = "1.0.0", currentVersion = "1.2.0")
        val savedSlot = slot<AppVersion>()

        every { appVersionRepository.findByPlatform(platform) } returns appVersion
        every { appVersionRepository.save(capture(savedSlot)) } returns appVersion

        // When
        useCase.execute(UpdateAppVersionCommand(platform = platform, minVersion = "1.1.0", currentVersion = "2.0.0"))

        // Then
        verify(exactly = 1) { appVersionRepository.save(any()) }
        savedSlot.captured.minVersion shouldBe "1.1.0"
        savedSlot.captured.currentVersion shouldBe "2.0.0"
    }

    test("존재하지 않는 platform - BusinessException(NOT_FOUND)을 던진다") {
        // Given
        val platform = Platform.ANDROID
        every { appVersionRepository.findByPlatform(platform) } returns null

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(UpdateAppVersionCommand(platform = platform, minVersion = "1.0.0", currentVersion = "2.0.0"))
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { appVersionRepository.save(any()) }
    }
})
