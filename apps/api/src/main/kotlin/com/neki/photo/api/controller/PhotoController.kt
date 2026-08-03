package com.neki.photo.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.common.domain.vo.SortOrder
import com.neki.photo.api.dto.PhotoImageConverter
import com.neki.photo.api.dto.PhotoImageRequest
import com.neki.photo.api.dto.PhotoImageResponse
import com.neki.photo.application.DeletePhotosUseCase
import com.neki.photo.application.GetPhotoUseCase
import com.neki.photo.application.GetPhotosUseCase
import com.neki.photo.application.PutPhotoUseCase
import com.neki.photo.application.UpdatePhotoUseCase
import com.neki.photo.application.UploadPhotosUseCase
import com.neki.photo.application.dto.PhotoImageResult
import com.neki.photo.dto.PhotoImageCommand
import com.neki.photo.dto.PhotoImageQuery
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
import org.springframework.web.bind.annotation.PutMapping
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
    private val getPhotoUseCase: GetPhotoUseCase,
    private val deletePhotosUseCase: DeletePhotosUseCase,
    private val updatePhotoUseCase: UpdatePhotoUseCase,
    private val putPhotoUseCase: PutPhotoUseCase,

    private val requestConverter: PhotoImageConverter.RequestConverter,
    private val responseConverter: PhotoImageConverter.ResponseConverter,
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
        @Valid @RequestBody request: PhotoImageRequest.UploadPhoto,
    ): BaseResponse<Any> {
        val command: PhotoImageCommand.UploadPhoto = requestConverter.toUploadPhotoCommand(userId, request)

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
    ): BaseResponse<PhotoImageResponse.GetPhotos> {
        val query: PhotoImageQuery.GetPhotos = requestConverter.toGetPhotosQuery(
            userId,
            folderId,
            page,
            size,
            sortOrder,
        )

        val result: PhotoImageResult.GetPhotos = getPhotosUseCase.execute(query)

        val response: PhotoImageResponse.GetPhotos = responseConverter.toGetPhotosResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "사진 상세 조회 API",
        description = "사진 상세 정보를 조회합니다.",
    )
    @GetMapping("/{photoId}")
    fun photoDetail(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable photoId: Long,
    ): BaseResponse<PhotoImageResponse.GetPhoto> {
        val query: PhotoImageQuery.GetPhoto = requestConverter.toGetPhotoQuery(userId, photoId)

        val result: PhotoImageResult.GetPhoto = getPhotoUseCase.execute(query)

        val response: PhotoImageResponse.GetPhoto = responseConverter.toGetPhotoResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "사진 삭제 API",
        description = "body에 포함된 사진들을 삭제합니다.",
    )
    @DeleteMapping
    fun deletePhotos(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: PhotoImageRequest.DeletePhotos,
    ): BaseResponse<Any> {
        val command: PhotoImageCommand.DeletePhotos = requestConverter.toDeletePhotosCommand(userId, request)

        deletePhotosUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "사진 갱신 API",
        description = "사진 정보를 갱신합니다. 기존 PATCH /api/photos/{photoId} 대신 사용해주세요.",
    )
    @PutMapping("/{photoId}")
    fun putPhoto(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable photoId: Long,
        @Valid @RequestBody request: PhotoImageRequest.UpdatePhoto,
    ): BaseResponse<Any> {
        val command: PhotoImageCommand.PutPhoto = requestConverter.toPutPhotoCommand(userId, photoId, request)

        putPhotoUseCase.execute(command)

        return BaseResponse()
    }

    @Deprecated(message = "PUT API 변경 후 제거")
    @Operation(
        summary = "사진 갱신 API",
        description = """사진 정보를 갱신합니다. @Deprecated

            null 처리를 용이하게 하기 위해 새롭게 PUT API를 추가하였습니다.
            API 변경 후 삭제 예정입니다. (PUT /api/photos/{photoId} 사용)
            변경 완료 후 서버 채널에 공유 부탁드립니다.
        """,
    )
    @PatchMapping("/{photoId}")
    fun updatePhoto(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable photoId: Long,
        @Valid @RequestBody request: PhotoImageRequest.UpdatePhoto,
    ): BaseResponse<Any> {
        val command: PhotoImageCommand.UpdatePhoto = requestConverter.toUpdatePhotoCommand(userId, photoId, request)

        updatePhotoUseCase.execute(command)

        return BaseResponse()
    }
}
