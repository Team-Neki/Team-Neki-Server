package com.neki.api.map.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.map.api.dto.MapConverter
import com.neki.api.map.api.dto.MapRequest
import com.neki.api.map.api.dto.MapV2Converter
import com.neki.api.map.api.dto.MapV2Response
import com.neki.api.map.application.GetPhotoBoothUseCase
import com.neki.api.map.application.dto.MapResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.map.dto.MapQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : MapV2Controller
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스 위치 정보 API V2. 지점 마스터 대신 매일 수집하는 지금 세대 포토부스(tb_photo_booth_enriched)를 본다.
 *   브랜드 조회·정렬 API 는 부스와 무관해 V1(/api/photo-booths/brand) 을 그대로 쓴다
 */
@RequiresSecurity
@Tag(name = "photo-booth v2", description = "포토부스 위치 API V2")
@RestController
@RequestMapping("/api/v2/photo-booths")
class MapV2Controller(
    private val getPhotoBoothUseCase: GetPhotoBoothUseCase,
    private val requestConverter: MapConverter.RequestConverter,
    private val responseConverter: MapV2Converter.ResponseConverter,
) {

    @Operation(
        summary = "다각형 영역 내 포토부스 조회 API V2",
        description = """
            다각형 영역 내의 포토부스 위치 정보를 조회합니다. 요청은 V1(POST /api/photo-booths/polygon) 과 같습니다.
            coordinates의 첫 좌표와 마지막 좌표는 동일해야 합니다 (다각형을 닫기 위함).

            * id 는 `source:<브랜드 코드>:<지점 ID>` 문자열입니다. V2 즐겨찾기 API 와 검색 부스 목록의 id 와 같은 값입니다
            * favorite 은 V1 에서 한 즐겨찾기도 포함합니다

            example에 있는 위치는 강남역 기준
            """,
    )
    @PostMapping("/polygon")
    fun getPhotoBoothsByPolygon(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: MapRequest.GetPolygonLocation,
    ): BaseResponse<MapV2Response.GetPolygonPhotoBooths> {
        val query: MapQuery.GetPolygonLocation = requestConverter.toGetPolygonLocationQuery(userId, request)

        val result: MapResult.GetPolygonPhotoBooths = getPhotoBoothUseCase.execute(query)

        val response: MapV2Response.GetPolygonPhotoBooths = responseConverter.toGetPolygonPhotoBoothsResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "특정 좌표 기준 반경 내 포토부스 조회 API V2",
        description = """
            특정 좌표를 기준으로 반경(radiusInMeters) 내의 포토부스를 거리순으로 조회합니다. 요청은 V1(POST /api/photo-booths/point) 과 같습니다.
            거리는 미터 단위 정수로 반환됩니다.
            radiusInMeters = 1000 (1KM)

            * id 는 `source:<브랜드 코드>:<지점 ID>` 문자열입니다. V2 즐겨찾기 API 와 검색 부스 목록의 id 와 같은 값입니다
            * favorite 은 V1 에서 한 즐겨찾기도 포함합니다

            example에 있는 위치는 강남역 기준
            """,
    )
    @PostMapping("/point")
    fun getPhotoBoothsByPoint(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: MapRequest.GetPointLocation,
    ): BaseResponse<MapV2Response.GetPointPhotoBooths> {
        val query: MapQuery.GetPointLocation = requestConverter.toGetPointLocationQuery(userId, request)

        val result: MapResult.GetPointPhotoBooths = getPhotoBoothUseCase.execute(query)

        val response: MapV2Response.GetPointPhotoBooths = responseConverter.toGetPointPhotoBoothsResponse(result)

        return BaseResponse(data = response)
    }
}
