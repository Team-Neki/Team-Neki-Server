package com.yapp2app.photo.api.controller

import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.photo.api.converter.PhotoImageCommandConverter
import com.yapp2app.photo.api.converter.PhotoImageResultConverter
import com.yapp2app.photo.api.dto.DeletePhotosRequest
import com.yapp2app.photo.api.dto.GetPhotosResponse
import com.yapp2app.photo.api.dto.UpdatePhotoRequest
import com.yapp2app.photo.api.dto.UploadPhotoRequest
import com.yapp2app.photo.application.usecase.DeletePhotosUseCase
import com.yapp2app.photo.application.usecase.GetPhotosUseCase
import com.yapp2app.photo.application.usecase.UpdatePhotoUseCase
import com.yapp2app.photo.application.usecase.UploadPhotosUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : PhotoController
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:23
 * description    : PhotoImage api endpoint
 */
@RequiresSecurity
@Tag(name = "photo image", description = "아카이빙 사진 API")
@RestController
@RequestMapping("/api/photos")
class PhotoController(
    private val uploadPhotosUseCase: UploadPhotosUseCase,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val deletePhotosUseCase: DeletePhotosUseCase,
    private val updatePhotoUseCase: UpdatePhotoUseCase,

    private val commandConverter: PhotoImageCommandConverter,
    private val resultConverter: PhotoImageResultConverter,
) {

    @Operation(
        summary = "사진 등록 API",
        description = """presigned 발급 API로 url을 발급받아 S3에 이미지들을 업로드한 후에 호출합니다.
            한 번에 최대 10장까지 업로드할 수 있습니다.
            모든 이미지가 S3에 업로드되었는지 검증 후에 메타데이터를 데이터베이스에 일괄 저장합니다.""",
    )
    @PostMapping
    fun uploadPhoto(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: UploadPhotoRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toUploadPhotoCommand(userId, request)
        uploadPhotosUseCase.execute(command)
        return BaseResponse()
    }

    @Operation(
        summary = "사진 목록 API",
        description = "사진 목록을 조회합니다. Offset 기반 페이징을 지원합니다.",
    )
    @GetMapping
    fun photoList(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam(required = false) folderId: Long?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
    ): BaseResponse<GetPhotosResponse> {
        val command = commandConverter.toGetPhotosCommand(userId, folderId, page, size, sortOrder)

        val result = getPhotosUseCase.execute(command)

        val response = resultConverter.toGetPhotosResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "사진 삭제 API",
        description = "body에 포함된 사진들을 삭제합니다. 단건 삭제는 photoIds에 하나의 ID만 전달하면 됩니다.",
    )
    @DeleteMapping
    fun deletePhotos(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: DeletePhotosRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toDeletePhotosCommand(userId, request)

        deletePhotosUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "사진 갱신 API",
        description = "사진 정보를 갱신합니다.",
    )
    @PatchMapping("/{photoId}")
    fun updatePhoto(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable photoId: Long,
        @Valid @RequestBody request: UpdatePhotoRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toUpdatePhotoCommand(userId, photoId, request)

        updatePhotoUseCase.execute(command)

        return BaseResponse()
    }
}
