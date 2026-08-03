package com.neki.map.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.map.api.dto.FavoriteMapConverter
import com.neki.map.api.dto.MapConverter
import com.neki.map.api.dto.MapRequest
import com.neki.map.api.dto.MapResponse
import com.neki.map.application.GetFavoriteMapsUseCase
import com.neki.map.application.UpdateMapFavoriteUseCase
import com.neki.map.application.dto.MapResult
import com.neki.map.dto.MapCommand
import com.neki.map.dto.MapQuery
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
 * fileName       : FavoriteMapController
 * author         : darren
 * date           : 2026. 6. 21.
 * description    : 지도(포토부스 위치) 즐겨찾기 API
 */
@RequiresSecurity
@Tag(name = "favorite photo-booth", description = "즐겨찾기 포토부스")
@RestController
@RequestMapping("/api/photo-booths")
class FavoriteMapController(
    private val updateMapFavoriteUseCase: UpdateMapFavoriteUseCase,
    private val getFavoriteMapsUseCase: GetFavoriteMapsUseCase,

    private val requestConverter: FavoriteMapConverter.RequestConverter,
    private val responseConverter: MapConverter.ResponseConverter,
) {
    @Operation(
        summary = "포토부스 즐겨찾기",
        description = "포토부스 위치를 즐겨찾기합니다. 멱등성 보장을 위해 body에 변경하고자하는 favorite 상태를 입력하면 됩니다.",
    )
    @PatchMapping("/{locationId}/favorite")
    fun favoriteMap(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable locationId: Long,
        @Valid @RequestBody request: MapRequest.UpdateMapFavorite,
    ): BaseResponse<Any> {
        val command: MapCommand.UpdateMapFavorite =
            requestConverter.toUpdateMapFavoriteCommand(userId, locationId, request)

        updateMapFavoriteUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "즐겨찾기한 포토부스 목록 조회",
        description = "사용자가 즐겨찾기한 포토부스를 즐겨찾기한 순서대로 조회합니다.",
    )
    @GetMapping("/favorite")
    fun getFavoriteMaps(
        @AuthenticationPrincipal(expression = "id") userId: Long,
    ): BaseResponse<MapResponse.GetFavoriteMap> {
        val query: MapQuery.GetFavoriteMaps = requestConverter.toGetFavoriteMapsQuery(userId)

        val result: MapResult.GetFavoriteMap = getFavoriteMapsUseCase.execute(query)

        val response: MapResponse.GetFavoriteMap = responseConverter.toGetFavoriteMapResponse(result)

        return BaseResponse(data = response)
    }
}
