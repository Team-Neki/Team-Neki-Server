package com.neki.map.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.map.api.converter.MapCommandConverter
import com.neki.map.api.converter.MapResultConverter
import com.neki.map.api.dto.CollectPhotoBoothRequest
import com.neki.map.api.dto.CollectPhotoBoothResponse
import com.neki.map.api.dto.GetBrandResponse
import com.neki.map.api.dto.GetPointLocationRequest
import com.neki.map.api.dto.GetPointLocationResponse
import com.neki.map.api.dto.GetPolygonLocationRequest
import com.neki.map.api.dto.GetPolygonLocationResponse
import com.neki.map.application.command.CollectPhotoBoothCommand
import com.neki.map.application.command.GetPointLocationCommand
import com.neki.map.application.command.GetPolygonLocationCommand
import com.neki.map.application.result.CollectPhotoBoothResult
import com.neki.map.application.result.GetBrandResult
import com.neki.map.application.result.GetPointLocationResult
import com.neki.map.application.result.GetPolygonLocationResult
import com.neki.map.application.usecase.CollectPhotoBoothLocationUseCase
import com.neki.map.application.usecase.GetBrandUseCase
import com.neki.map.application.usecase.GetPhotoBoothLocationUseCase
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : MapController
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 위치 정보 API
 */
@RequiresSecurity
@Tag(name = "photo-booth", description = "포토부스 위치 API")
@RestController
@RequestMapping("/api/photo-booths")
class MapController(
    private val getBrandUseCase: GetBrandUseCase,
    private val collectPhotoBoothLocationUseCase: CollectPhotoBoothLocationUseCase,
    private val getPhotoBoothLocationUseCase: GetPhotoBoothLocationUseCase,
    private val commandConverter: MapCommandConverter,
    private val resultConverter: MapResultConverter,
) {

    @Operation(
        summary = "브랜드 종류 조회 API",
        description = """
            브랜드 종류 및 이미지를 조회합니다.
            """,
    )
    @GetMapping("/brand")
    fun getBrand(): BaseResponse<List<GetBrandResponse>> {
        val result: List<GetBrandResult> = getBrandUseCase.execute()

        val response: List<GetBrandResponse> = resultConverter.toGetBrandResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "포토부스 위치 수집 API",
        description = """
            Kakao Local API를 통해 포토부스 위치 정보를 수집하여 DB에 저장합니다.
            첫 페이지 조회 후 meta.pageable_count를 기반으로 자동으로 모든 페이지를 순회합니다.
            """,
    )
    @Hidden
    @PostMapping("/collect")
    fun collectPhotoBooths(
        @Valid @RequestBody request: CollectPhotoBoothRequest,
    ): BaseResponse<CollectPhotoBoothResponse> {
        val command: CollectPhotoBoothCommand = commandConverter.toCollectPhotoBoothCommand(request)

        val result: CollectPhotoBoothResult = collectPhotoBoothLocationUseCase.execute(command)

        val response: CollectPhotoBoothResponse = resultConverter.toCollectPhotoBoothResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "다각형 영역 내 포토부스 조회 API",
        description = """
            다각형 영역 내의 포토부스 위치 정보를 조회합니다.
            coordinates의 첫 좌표와 마지막 좌표는 동일해야 합니다 (다각형을 닫기 위함).

            example에 있는 위치는 강남역 기준
            """,
    )
    @PostMapping("/polygon")
    fun getPhotoBoothsByPolygon(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: GetPolygonLocationRequest,
    ): BaseResponse<GetPolygonLocationResponse> {
        val command: GetPolygonLocationCommand = commandConverter.toGetPolygonLocationCommand(userId, request)

        val result: GetPolygonLocationResult = getPhotoBoothLocationUseCase.execute(command)

        val response: GetPolygonLocationResponse = resultConverter.toGetPolygonLocationResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "특정 좌표 기준 반경 내 포토부스 조회 API",
        description = """
            특정 좌표를 기준으로 반경(radiusInMeters) 내의 포토부스를 거리순으로 조회합니다.
            거리는 미터 단위 정수로 반환됩니다.
            radiusInMeters = 1000 (1KM)
            example에 있는 위치는 강남역 기준
            """,
    )
    @PostMapping("/point")
    fun getPhotoBoothsByPoint(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: GetPointLocationRequest,
    ): BaseResponse<GetPointLocationResponse> {
        val command: GetPointLocationCommand = commandConverter.toGetPointLocationCommand(userId, request)

        val result: GetPointLocationResult = getPhotoBoothLocationUseCase.execute(command)

        val response: GetPointLocationResponse = resultConverter.toGetPointLocationResponse(result)

        return BaseResponse(data = response)
    }
}
