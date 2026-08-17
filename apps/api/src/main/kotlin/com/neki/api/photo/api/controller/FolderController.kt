package com.neki.api.photo.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.photo.api.dto.FolderConverter
import com.neki.api.photo.api.dto.FolderRequest
import com.neki.api.photo.api.dto.FolderResponse
import com.neki.api.photo.application.CopyPhotosToFolderUseCase
import com.neki.api.photo.application.CreateFolderUseCase
import com.neki.api.photo.application.DeleteFoldersUseCase
import com.neki.api.photo.application.GetFoldersUseCase
import com.neki.api.photo.application.MovePhotosToFolderUseCase
import com.neki.api.photo.application.RemovePhotosFromFolderUseCase
import com.neki.api.photo.application.UpdateFolderUseCase
import com.neki.api.photo.application.dto.FolderResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.photo.dto.FolderCommand
import com.neki.domain.photo.dto.FolderQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
    private val movePhotosToFolderUseCase: MovePhotosToFolderUseCase,
    private val copyPhotosToFolderUseCase: CopyPhotosToFolderUseCase,

    private val requestConverter: FolderConverter.RequestConverter,
    private val responseConverter: FolderConverter.ResponseConverter,
) {

    @Operation(
        summary = "폴더 생성 API",
        description = "폴더를 생성합니다.",
    )
    @PostMapping
    fun createFolder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: FolderRequest.CreateFolder,
    ): BaseResponse<FolderResponse.CreateFolder> {
        val command: FolderCommand.CreateFolder = requestConverter.toCreateFolderCommand(request, userId)

        val result: FolderResult.CreateFolder = createFolderUseCase.execute(command)

        val response: FolderResponse.CreateFolder = responseConverter.toCreateFolderResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "폴더 목록 조회 API",
        description = "폴더 목록을 조회합니다.",
    )
    @GetMapping
    fun getAllFolder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam("limit") @Min(1) limit: Int?,
    ): BaseResponse<FolderResponse.GetAllFolder> {
        val query: FolderQuery.GetFolders = requestConverter.toGetFoldersQuery(userId, limit)

        val result: FolderResult.GetFolders = getFoldersUseCase.execute(query)

        val response: FolderResponse.GetAllFolder = responseConverter.toGetAllFoldersResponse(result)

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
        @Valid @RequestBody request: FolderRequest.DeleteFolders,
    ): BaseResponse<Any> {
        val command: FolderCommand.DeleteFolders = requestConverter.toDeleteFoldersCommand(
            request,
            userId,
            deletePhotos,
        )

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
        @Valid @RequestBody request: FolderRequest.UpdateFolder,
    ): BaseResponse<Any> {
        val command: FolderCommand.UpdateFolder = requestConverter.toUpdateFolderCommand(request, folderId, userId)

        updateFolderUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "사진 이동 API",
        description = "사진을 source 폴더에서 target 폴더로 이동합니다.",
    )
    @PatchMapping("/photos/move")
    fun movePhotosToFolder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: FolderRequest.MovePhotosToFolder,
    ): BaseResponse<Any> {
        val command: FolderCommand.MovePhotosToFolder = requestConverter.toMovePhotosToFolderCommand(
            request,
            userId,
        )

        movePhotosToFolderUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "사진 복제 API",
        description = "사진을 source 폴더에서 target 폴더로 복제합니다. source 폴더의 사진은 유지됩니다.",
    )
    @PostMapping("/photos/copy")
    fun copyPhotosToFolder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: FolderRequest.CopyPhotosToFolder,
    ): BaseResponse<Any> {
        val command: FolderCommand.CopyPhotosToFolder = requestConverter.toCopyPhotosToFolderCommand(
            request,
            userId,
        )

        copyPhotosToFolderUseCase.execute(command)

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
        @Valid @RequestBody request: FolderRequest.RemovePhotosFromFolder,
    ): BaseResponse<Any> {
        val command: FolderCommand.RemovePhotosFromFolder = requestConverter.toRemovePhotosFromFolderCommand(
            request,
            folderId,
            userId,
        )

        removePhotosFromFolderUseCase.execute(command)

        return BaseResponse()
    }
}
