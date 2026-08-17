package com.neki.api.support.application.usecase

import com.neki.api.support.application.GetAppVersionUseCase
import com.neki.api.testfixture.anAppVersion
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.support.dto.AppVersionQuery
import com.neki.domain.support.models.Platform
import com.neki.domain.support.repository.AppVersionRepository
import com.neki.domain.support.service.AppVersionService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetAppVersionUseCaseTest {

    private lateinit var appVersionRepository: AppVersionRepository
    private lateinit var useCase: GetAppVersionUseCase

    @BeforeEach
    fun setUp() {
        appVersionRepository = mockk()
        useCase = GetAppVersionUseCase(AppVersionService(appVersionRepository))
    }

    @Test
    @DisplayName("정상 조회 - port가 AppVersion 반환 시 result가 올바르게 매핑된다")
    fun `정상 조회 - port가 AppVersion 반환 시 result가 올바르게 매핑된다`() {
        // Given
        val platform = Platform.IOS
        val appVersion = anAppVersion(platform = platform, minVersion = "1.0.0", currentVersion = "2.0.0")
        every { appVersionRepository.findByPlatform(platform) } returns appVersion

        // When
        val result = useCase.execute(AppVersionQuery.GetAppVersion(platform = platform))

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
            useCase.execute(AppVersionQuery.GetAppVersion(platform = platform))
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND
    }
}
