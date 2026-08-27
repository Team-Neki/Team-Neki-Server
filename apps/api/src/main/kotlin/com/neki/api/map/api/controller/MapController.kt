package com.neki.api.map.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.map.api.dto.MapConverter
import com.neki.api.map.api.dto.MapRequest
import com.neki.api.map.api.dto.MapResponse
import com.neki.api.map.application.CollectPhotoBoothLocationUseCase
import com.neki.api.map.application.GetBrandUseCase
import com.neki.api.map.application.GetFilterUseCase
import com.neki.api.map.application.GetPhotoBoothLocationUseCase
import com.neki.api.map.application.UpdateBrandOrderUseCase
import com.neki.api.map.application.dto.MapResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.dto.MapQuery
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
    private val updateBrandOrderUseCase: UpdateBrandOrderUseCase,
    private val collectPhotoBoothLocationUseCase: CollectPhotoBoothLocationUseCase,
    private val getPhotoBoothLocationUseCase: GetPhotoBoothLocationUseCase,
    private val getFilterUseCase: GetFilterUseCase,
    private val requestConverter: MapConverter.RequestConverter,
    private val responseConverter: MapConverter.ResponseConverter,
) {

    @Operation(
        summary = "브랜드 종류 조회 API",
        description = """
            브랜드 종류 및 이미지를 조회합니다.
            사용자가 정렬 순서를 저장한 경우(PUT /api/photo-booths/brand/order) 저장한 순서대로,
            저장하지 않은 경우 서버 기본 순서대로 반환합니다.
            """,
    )
    @GetMapping("/brand")
    fun getBrand(@AuthenticationPrincipal(expression = "id") userId: Long): BaseResponse<List<MapResponse.GetBrand>> {
        val result: List<MapResult.GetBrand> = getBrandUseCase.execute(MapQuery.GetBrand(userId))

        val response: List<MapResponse.GetBrand> = responseConverter.toGetBrandResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "브랜드 정렬 순서 저장 API",
        description = """
            사용자가 커스텀한 브랜드 정렬 순서를 저장합니다.
            brandIds 에 보여주고자 하는 순서대로 브랜드 ID 를 전달하면, 이후 브랜드 조회 API 가 해당 순서로 반환합니다.
            전체 순서를 덮어쓰는 방식(멱등)이며, 다시 호출하면 기존 순서는 새 순서로 대체됩니다.
            """,
    )
    @PutMapping("/brand/order")
    fun updateBrandOrder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: MapRequest.UpdateBrandOrder,
    ): BaseResponse<Any> {
        val command: MapCommand.UpdateBrandOrder = requestConverter.toUpdateBrandOrderCommand(userId, request)

        updateBrandOrderUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "포토부스 위치 수집 API",
        description = """
            Kakao Local API를 통해 포토부스 위치 정보를 수집하여 DB에 저장합니다.
            첫 페이지 조회 후 meta.pageable_count를 기반으로 자동으로 모든 페이지를 순회합니다.
            """,
        deprecated = true,
    )
    @Deprecated("Prefect 배치로 이관 예정. 이관 완료 후 제거한다")
    @Hidden
    @PostMapping("/collect")
    fun collectPhotoBooths(
        @Valid @RequestBody request: MapRequest.CollectPhotoBooth,
    ): BaseResponse<MapResponse.CollectPhotoBooth> {
        val command: MapCommand.CollectPhotoBooth = requestConverter.toCollectPhotoBoothCommand(request)

        val result: MapResult.CollectPhotoBooth = collectPhotoBoothLocationUseCase.execute(command)

        val response: MapResponse.CollectPhotoBooth = responseConverter.toCollectPhotoBoothResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "다각형 영역 내 포토부스 조회 API",
        description = """
            다각형 영역 내의 포토부스 위치 정보를 조회합니다.
            요청은 필터별로 그룹지어 전달합니다. polygonFilter 는 필수, 나머지 필터는 생략할 수 있습니다.
            polygonFilter.coordinates 는 4개 이상이어야 하고 첫 좌표와 마지막 좌표는 동일해야 합니다 (다각형을 닫기 위함). 위반 시 400 을 반환합니다.

            example에 있는 위치는 강남역 기준
            """,
    )
    @PostMapping("/polygon")
    fun getPhotoBoothsByPolygon(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: MapRequest.FilterGroup,
    ): BaseResponse<MapResponse.GetPolygonLocation> {
        val query: MapQuery.GetPolygonLocation = requestConverter.toGetPolygonLocationQuery(userId, request)

        val result: MapResult.GetPolygonLocation = getPhotoBoothLocationUseCase.execute(query)

        val response: MapResponse.GetPolygonLocation = responseConverter.toGetPolygonLocationResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "다각형 영역 내 지도 필터 조회 API",
        description = """
            다각형 영역에서 사용할 수 있는 지도 필터를 조회합니다.
            현재 화면에 실제로 존재하는 대상만 필터에 노출하기 위한 API 입니다.

            brandFilter 는 영역 내에 포토부스가 존재하는 브랜드 목록입니다.
            필터가 추가되면 응답에 필드가 늘어나므로, 클라이언트는 필요한 필드만 읽으면 됩니다.

            요청은 필터별로 그룹지어 전달합니다. polygonFilter 는 필수, 나머지 필터는 생략할 수 있습니다.
            polygonFilter.coordinates 는 4개 이상이어야 하고 첫 좌표와 마지막 좌표는 동일해야 합니다 (다각형을 닫기 위함). 위반 시 400 을 반환합니다.

            brandFilter 의 정렬 순서는 브랜드 전체 조회(GET /api/photo-booths/brand)와 동일하게 사용자별 정렬 순서를 따릅니다.
            count 는 해당 영역 안에 있는 그 브랜드의 포토부스 개수입니다.
            브랜드 이미지는 내려주지 않습니다. 브랜드 전체 조회에서 받은 값을 id 로 매칭해 재사용하세요.

            example에 있는 위치는 강남역 기준
            """,
    )
    @PostMapping("/polygon/filter")
    fun getFilter(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: MapRequest.FilterGroup,
    ): BaseResponse<MapResponse.PolygonFilter> {
        val query: MapQuery.PolygonFilter = requestConverter.toPolygonFilterQuery(userId, request)

        val result: MapResult.PolygonFilter = getFilterUseCase.execute(query)

        val response: MapResponse.PolygonFilter = responseConverter.toPolygonFilterResponse(result)

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
        @Valid @RequestBody request: MapRequest.GetPointLocation,
    ): BaseResponse<MapResponse.GetPointLocation> {
        val query: MapQuery.GetPointLocation = requestConverter.toGetPointLocationQuery(userId, request)

        val result: MapResult.GetPointLocation = getPhotoBoothLocationUseCase.execute(query)

        val response: MapResponse.GetPointLocation = responseConverter.toGetPointLocationResponse(result)

        return BaseResponse(data = response)
    }
}
