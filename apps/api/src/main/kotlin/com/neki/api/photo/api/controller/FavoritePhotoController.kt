package com.neki.api.photo.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.photo.api.dto.FavoritePhotoConverter
import com.neki.api.photo.api.dto.PhotoImageConverter
import com.neki.api.photo.api.dto.PhotoImageRequest
import com.neki.api.photo.api.dto.PhotoImageResponse
import com.neki.api.photo.application.GetFavoritePhotosUseCase
import com.neki.api.photo.application.GetFavoriteSummaryUseCase
import com.neki.api.photo.application.UpdatePhotoFavoriteUseCase
import com.neki.api.photo.application.dto.PhotoImageResult
import com.neki.core.api.dto.BaseResponse
import com.neki.core.domain.vo.SortOrder
import com.neki.domain.photo.dto.PhotoImageCommand
import com.neki.domain.photo.dto.PhotoImageQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : FavoritePhotoController
 * author         : koo
 * date           : 2026. 1. 14. 오전 2:22
 * description    :
 */
@RequiresSecurity
@Tag(name = "favorite image", description = "즐겨찾기 사진")
@RestController
@RequestMapping("/api/photos")
class FavoritePhotoController(
    private val getFavoritePhotosUseCase: GetFavoritePhotosUseCase,
    private val getFavoriteSummaryUseCase: GetFavoriteSummaryUseCase,
    private val updatePhotoFavoriteUseCase: UpdatePhotoFavoriteUseCase,

    private val requestConverter: FavoritePhotoConverter.RequestConverter,
    private val responseConverter: PhotoImageConverter.ResponseConverter,
) {
    @Operation(
        summary = "이미지 즐겨찾기",
        description = "이미지를 즐겨찾기합니다. 멱등성 보장을 위해 body에 변경하고자하는 favorite 상태를 입력하면 됩니다.",
    )
    @PatchMapping("/{photoId}/favorite")
    fun favoritePhoto(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable photoId: Long,
        @Valid @RequestBody request: PhotoImageRequest.UpdatePhotoFavorite,
    ): BaseResponse<Any> {
        val command: PhotoImageCommand.UpdatePhotoFavorite =
            requestConverter.toUpdatePhotoFavoriteCommand(userId, photoId, request)

        updatePhotoFavoriteUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "즐겨찾기 사진 요약 정보 조회",
        description = "가장 최근에 즐겨찾기한 사진의 이미지 URL과 총 즐겨찾기 개수를 조회합니다.",
    )
    @GetMapping("/favorite/summary")
    fun getFavoriteSummary(
        @AuthenticationPrincipal(expression = "id") userId: Long,
    ): BaseResponse<PhotoImageResponse.GetFavoriteSummary> {
        val query: PhotoImageQuery.GetFavoriteSummary = requestConverter.toGetFavoriteSummaryQuery(userId)

        val result: PhotoImageResult.GetFavoriteSummary = getFavoriteSummaryUseCase.execute(query)

        val response: PhotoImageResponse.GetFavoriteSummary = responseConverter.toGetFavoriteSummaryResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "즐겨찾기 이미지 조회",
        description = "즐겨찾기한 이미지 목록을 조회합니다. Offset 기반 페이징과 정렬을 지원합니다.",
    )
    @GetMapping("/favorite")
    fun favoritePhotoList(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
    ): BaseResponse<PhotoImageResponse.GetPhotos> {
        val query: PhotoImageQuery.GetFavoritePhotos =
            requestConverter.toGetFavoritePhotosQuery(userId, page, size, sortOrder)

        val result: PhotoImageResult.GetPhotos = getFavoritePhotosUseCase.execute(query)

        val response: PhotoImageResponse.GetPhotos = responseConverter.toGetPhotosResponse(result)

        return BaseResponse(data = response)
    }
}
