package com.neki.api.map.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.map.api.dto.FavoriteMapConverter
import com.neki.api.map.api.dto.MapRequest
import com.neki.api.map.api.dto.MapV2Converter
import com.neki.api.map.api.dto.MapV2Response
import com.neki.api.map.application.GetFavoritePhotoBoothsUseCase
import com.neki.api.map.application.UpdatePhotoBoothFavoriteUseCase
import com.neki.api.map.application.dto.MapResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.dto.MapQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : FavoriteMapV2Controller
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스 즐겨찾기 API V2. 부스를 포토부스 키 문자열로 가리킨다
 */
@RequiresSecurity
@Tag(name = "favorite photo-booth v2", description = "즐겨찾기 포토부스 V2")
@RestController
@RequestMapping("/api/v2/photo-booths")
class FavoriteMapV2Controller(
    private val updatePhotoBoothFavoriteUseCase: UpdatePhotoBoothFavoriteUseCase,
    private val getFavoritePhotoBoothsUseCase: GetFavoritePhotoBoothsUseCase,

    private val requestConverter: MapV2Converter.RequestConverter,
    private val favoriteMapRequestConverter: FavoriteMapConverter.RequestConverter,
    private val responseConverter: MapV2Converter.ResponseConverter,
) {
    @Operation(
        summary = "포토부스 즐겨찾기 V2",
        description = """
            포토부스를 즐겨찾기합니다. 멱등성 보장을 위해 body에 변경하고자하는 favorite 상태를 입력하면 됩니다.

            * photoBoothId 는 V2 조회·검색 부스 목록 응답의 id(`source:<브랜드 코드>:<지점 ID>`) 입니다. 모양이 맞지 않으면 D-01
            * 지금 수집 데이터에 없는 부스면 D-04
            * 해제하면 V1 에서 한 즐겨찾기도 함께 해제되어 V1·V2 어느 쪽에서도 즐겨찾기가 아닙니다
            """,
    )
    @PatchMapping("/{photoBoothId}/favorite")
    fun favoritePhotoBooth(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable photoBoothId: String,
        @Valid @RequestBody request: MapRequest.UpdateMapFavorite,
    ): BaseResponse<Any> {
        val command: MapCommand.UpdatePhotoBoothFavorite =
            requestConverter.toUpdatePhotoBoothFavoriteCommand(userId, photoBoothId, request)

        updatePhotoBoothFavoriteUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "즐겨찾기한 포토부스 목록 조회 V2",
        description = """
            사용자가 즐겨찾기한 포토부스를 최근 즐겨찾기한 순서대로 조회합니다.

            * V1 에서 한 즐겨찾기도 포함합니다
            * 지금 수집 데이터에 없는(폐점 등) 부스는 나오지 않습니다
            """,
    )
    @GetMapping("/favorite")
    fun getFavoritePhotoBooths(
        @AuthenticationPrincipal(expression = "id") userId: Long,
    ): BaseResponse<MapV2Response.GetFavoritePhotoBooths> {
        val query: MapQuery.GetFavoriteMaps = favoriteMapRequestConverter.toGetFavoriteMapsQuery(userId)

        val result: MapResult.GetFavoritePhotoBooths = getFavoritePhotoBoothsUseCase.execute(query)

        val response: MapV2Response.GetFavoritePhotoBooths = responseConverter.toGetFavoritePhotoBoothsResponse(result)

        return BaseResponse(data = response)
    }
}
