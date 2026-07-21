package com.neki.support.api.controller

import com.neki.common.api.dto.BaseResponse
import com.neki.support.api.converter.AppVersionCommandConverter
import com.neki.support.api.converter.AppVersionResultConverter
import com.neki.support.api.dto.GetAppVersionResponse
import com.neki.support.api.dto.UpdateAppVersionRequest
import com.neki.support.application.dto.AppVersionCommand
import com.neki.support.application.dto.AppVersionQuery
import com.neki.support.application.dto.AppVersionResult
import com.neki.support.application.usecase.GetAppVersionUseCase
import com.neki.support.application.usecase.UpdateAppVersionUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : VersionController
 * author         : darren
 * date           : 2026. 1. 29
 * description    : Version api endpoint
 */
@Tag(name = "version", description = "앱 버전 API")
@RestController
@RequestMapping("/api/versions")
class VersionController(
    private val getAppVersionUseCase: GetAppVersionUseCase,
    private val updateAppVersionUseCase: UpdateAppVersionUseCase,
    private val commandConverter: AppVersionCommandConverter,
    private val resultConverter: AppVersionResultConverter,
) {

    @Operation(
        summary = "앱 버전 조회 API",
        description = "플랫폼별 최소 버전 및 현재 버전을 조회합니다. (android, ios)",
    )
    @GetMapping("/{platform}")
    fun getAppVersion(@PathVariable platform: String): BaseResponse<GetAppVersionResponse> {
        val query: AppVersionQuery.GetAppVersion = commandConverter.toGetAppVersionQuery(platform)

        val result: AppVersionResult.GetAppVersion = getAppVersionUseCase.execute(query)

        val response: GetAppVersionResponse = resultConverter.toGetAppVersionResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "[프론트분들 테스트용] 앱 버전 수정 API",
        description = "플랫폼별 최소 버전 및 현재 버전을 수정합니다. (android, ios)",
    )
    @PatchMapping("/{platform}")
    fun updateAppVersion(
        @PathVariable platform: String,
        @Valid @RequestBody request: UpdateAppVersionRequest,
    ): BaseResponse<Any> {
        val command: AppVersionCommand.UpdateAppVersion = commandConverter.toUpdateAppVersionCommand(platform, request)

        updateAppVersionUseCase.execute(command)

        return BaseResponse()
    }
}
