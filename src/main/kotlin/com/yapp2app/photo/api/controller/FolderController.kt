package com.yapp2app.photo.api.controller

import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.photo.api.converter.FolderCommandConverter
import com.yapp2app.photo.api.converter.FolderResultConverter
import com.yapp2app.photo.api.dto.CreateFolderRequest
import com.yapp2app.photo.api.dto.CreateFolderResponse
import com.yapp2app.photo.api.dto.DeleteFoldersRequest
import com.yapp2app.photo.api.dto.GetAllFolderResponse
import com.yapp2app.photo.api.dto.RemovePhotosFromFolderRequest
import com.yapp2app.photo.api.dto.UpdateFolderRequest
import com.yapp2app.photo.application.usecase.CreateFolderUseCase
import com.yapp2app.photo.application.usecase.DeleteFoldersUseCase
import com.yapp2app.photo.application.usecase.GetFoldersUseCase
import com.yapp2app.photo.application.usecase.RemovePhotosFromFolderUseCase
import com.yapp2app.photo.application.usecase.UpdateFolderUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
 * fileName       : FolderController
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:58
 * description    : Folder aggregate에 대한 api endpoint
 */
@RequiresSecurity
@Tag(name = "folder", description = "폴더 API")
@RestController
@RequestMapping("/api/folders")
class FolderController(
    private val createFolderUseCase: CreateFolderUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val deleteFoldersUseCase: DeleteFoldersUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val removePhotosFromFolderUseCase: RemovePhotosFromFolderUseCase,
    private val commandConverter: FolderCommandConverter,
    private val resultConverter: FolderResultConverter,
) {

    @Operation(
        summary = "폴더 생성 API",
        description = "폴더를 생성합니다.",
    )
    @PostMapping
    fun createFolder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: CreateFolderRequest,
    ): BaseResponse<CreateFolderResponse> {
        val command = commandConverter.toCreateFolderCommand(request, userId)

        val result = createFolderUseCase.execute(command)

        val response = resultConverter.toCreateFolderResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "폴더 목록 조회 API",
        description = "폴더 목록을 조회합니다.",
    )
    @GetMapping
    fun getAllFolder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam("limit") limit: Int?,
    ): BaseResponse<GetAllFolderResponse> {
        val command = commandConverter.toGetFoldersCommand(userId, limit)

        val result = getFoldersUseCase.execute(command)

        val response = resultConverter.toGetAllFoldersResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "폴더 삭제 API",
        description = "폴더를 삭제합니다. deletePhotos=true이면 폴더 내 사진까지 완전 삭제합니다.",
    )
    @DeleteMapping
    fun deleteFolders(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam(defaultValue = "false") deletePhotos: Boolean,
        @Valid @RequestBody request: DeleteFoldersRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toDeleteFoldersCommand(request, userId, deletePhotos)

        deleteFoldersUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "폴더 갱신 API",
        description = "폴더 정보를 갱신합니다.",
    )
    @PatchMapping("/{folderId}")
    fun updateFolder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable folderId: Long,
        @Valid @RequestBody request: UpdateFolderRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toUpdateFolderCommand(request, folderId, userId)

        updateFolderUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "폴더에서 사진 제외 API",
        description = "폴더에서 사진을 제외합니다. 사진 자체는 삭제되지 않고 폴더와의 연관관계만 해제됩니다.",
    )
    @DeleteMapping("/{folderId}/photos")
    fun removePhotosFromFolder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable folderId: Long,
        @Valid @RequestBody request: RemovePhotosFromFolderRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toRemovePhotosFromFolderCommand(request, folderId, userId)

        removePhotosFromFolderUseCase.execute(command)

        return BaseResponse()
    }
}
