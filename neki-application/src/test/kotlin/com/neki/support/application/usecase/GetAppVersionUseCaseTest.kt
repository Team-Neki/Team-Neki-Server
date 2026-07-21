package com.neki.support.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.application.command.GetAppVersionCommand
import com.neki.support.application.port.AppVersionRepositoryPort
import com.neki.support.enums.Platform
import com.neki.testfixture.anAppVersion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetAppVersionUseCaseTest {

    private lateinit var appVersionRepository: AppVersionRepositoryPort
    private lateinit var useCase: GetAppVersionUseCase

    @BeforeEach
    fun setUp() {
        appVersionRepository = mockk()
        useCase = GetAppVersionUseCase(appVersionRepository)
    }

    @Test
    @DisplayName("정상 조회 - port가 AppVersion 반환 시 result가 올바르게 매핑된다")
    fun `정상 조회 - port가 AppVersion 반환 시 result가 올바르게 매핑된다`() {
        // Given
        val platform = Platform.IOS
        val appVersion = anAppVersion(platform = platform, minVersion = "1.0.0", currentVersion = "2.0.0")
        every { appVersionRepository.findByPlatform(platform) } returns appVersion

        // When
        val result = useCase.execute(GetAppVersionCommand(platform = platform))

        // Then
        result.platform shouldBe platform
        result.minVersion shouldBe "1.0.0"
        result.currentVersion shouldBe "2.0.0"
    }

    @Test
    @DisplayName("존재하지 않는 platform - port가 null 반환 시 BusinessException(NOT_FOUND)을 던진다")
    fun `존재하지 않는 platform - port가 null 반환 시 BusinessException(NOT_FOUND)을 던진다`() {
        // Given
        val platform = Platform.ANDROID
        every { appVersionRepository.findByPlatform(platform) } returns null

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(GetAppVersionCommand(platform = platform))
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND
    }
}
