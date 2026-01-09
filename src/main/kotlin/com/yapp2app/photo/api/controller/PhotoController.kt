package com.yapp2app.photo.api.controller

import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.photo.api.converter.PhotoImageCommandConverter
import com.yapp2app.photo.api.converter.PhotoImageResultConverter
import com.yapp2app.photo.api.dto.DeletePhotosRequest
import com.yapp2app.photo.api.dto.GetPhotosResponse
import com.yapp2app.photo.api.dto.UpdatePhotoRequest
import com.yapp2app.photo.api.dto.UploadPhotoRequest
import com.yapp2app.photo.api.dto.UploadPhotoResponse
import com.yapp2app.photo.application.usecase.DeletePhotoUseCase
import com.yapp2app.photo.application.usecase.GetPhotosUseCase
import com.yapp2app.photo.application.usecase.UpdatePhotoUseCase
import com.yapp2app.photo.application.usecase.UploadPhotoUseCase
import jakarta.validation.Valid
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
@RestController
@RequestMapping("/api/photos")
class PhotoController(
    private val uploadPhotoUseCase: UploadPhotoUseCase,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val deletePhotoUseCase: DeletePhotoUseCase,
    private val updatePhotoUseCase: UpdatePhotoUseCase,

    private val commandConverter: PhotoImageCommandConverter,
    private val resultConverter: PhotoImageResultConverter,
) {

    @PostMapping
    fun uploadPhoto(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: UploadPhotoRequest,
    ): BaseResponse<UploadPhotoResponse> {
        val command = commandConverter.toUploadPhotoCommand(userId, request)

        val result = uploadPhotoUseCase.execute(command)

        val response = resultConverter.toUploadPhotoResponse(result)

        return BaseResponse(data = response)
    }

    @GetMapping
    fun photoList(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam(required = false) folderId: Long?,
    ): BaseResponse<GetPhotosResponse> {
        val command = commandConverter.toGetPhotosCommand(userId, folderId)

        val result = getPhotosUseCase.execute(command)

        val response = resultConverter.toGetPhotosResponse(result)

        return BaseResponse(data = response)
    }

    @DeleteMapping("/{photoId}")
    fun deletePhoto(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable photoId: Long,
    ): BaseResponse<Any> {
        val command = commandConverter.toDeletePhotoCommand(userId, photoId)

        deletePhotoUseCase.execute(command)

        return BaseResponse()
    }

    @DeleteMapping
    fun deletePhotos(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: DeletePhotosRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toDeletePhotosCommand(userId, request)

        deletePhotoUseCase.execute(command)

        return BaseResponse()
    }

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
