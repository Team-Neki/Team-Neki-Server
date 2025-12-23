package com.yapp2app.photobooth.api.controller

import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.photobooth.api.request.CreateFolderRequest
import com.yapp2app.photobooth.api.request.DeleteFoldersRequest
import com.yapp2app.photobooth.api.request.UpdateFolderRequest
import com.yapp2app.photobooth.api.response.GetAllFolderResponse
import com.yapp2app.photobooth.application.command.CreateFolderCommand
import com.yapp2app.photobooth.application.command.DeleteFolderCommand
import com.yapp2app.photobooth.application.command.DeleteFoldersCommand
import com.yapp2app.photobooth.application.command.GetFoldersCommand
import com.yapp2app.photobooth.application.command.UpdateFolderCommand
import com.yapp2app.photobooth.application.usecase.CreateFolderUseCase
import com.yapp2app.photobooth.application.usecase.DeleteFolderUseCase
import com.yapp2app.photobooth.application.usecase.GetFoldersUseCase
import com.yapp2app.photobooth.application.usecase.UpdateFolderUseCase
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
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : FolderController
 * author         : koo
 * date           : 2025. 12. 23. 오후 7:58
 * description    : Folder aggregate에 대한 api endpoint
 */
@Tag(name = "folder", description = "폴더 API")
@RestController
@RequestMapping("/api/folders")
class FolderController(
    private val createFolderUseCase: CreateFolderUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
) {

    @Operation(
        summary = "폴더 생성 API",
        description = "폴더를 생성합니다.",
    )
    @PostMapping
    fun createFolder(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody body: CreateFolderRequest,
    ): BaseResponse<Any> {
        createFolderUseCase.execute(CreateFolderCommand(userId, body.name))

        return BaseResponse()
    }

    @Operation(
        summary = "폴더 목록 조회 API",
        description = "폴더 목록을 조회합니다.",
    )
    @GetMapping
    fun getAllFolder(@AuthenticationPrincipal userId: Long): BaseResponse<GetAllFolderResponse> {
        val result = getFoldersUseCase.execute(GetFoldersCommand(userId))

        return BaseResponse(
            data = GetAllFolderResponse(
                result.items.map {
                    GetAllFolderResponse.FolderInfo(
                        it.folderId,
                        it.name,
                    )
                },
            ),
        )
    }

    @Operation(
        summary = "폴더 삭제 API",
        description = "단건 폴더 삭제를 합니다.",
    )
    @DeleteMapping("/{folderId}")
    fun deleteFolder(@AuthenticationPrincipal userId: Long, @PathVariable folderId: Long): BaseResponse<Any> {
        deleteFolderUseCase.execute(DeleteFolderCommand(userId, folderId))

        return BaseResponse()
    }

    @Operation(
        summary = "폴더 선택 삭제 API",
        description = "여러 개의 폴더를 선택하여 삭제합니다.",
    )
    @DeleteMapping
    fun deleteFolders(
        @AuthenticationPrincipal userId: Long,
        @RequestBody body: DeleteFoldersRequest,
    ): BaseResponse<Any> {
        deleteFolderUseCase.execute(DeleteFoldersCommand(userId, body.folderIds))

        return BaseResponse()
    }

    @Operation(
        summary = "폴더 갱신 API",
        description = "폴더 정보를 갱신합니다.",
    )
    @PatchMapping("/{folderId}")
    fun updateFolder(
        @AuthenticationPrincipal userId: Long,
        @PathVariable folderId: Long,
        @RequestBody body: UpdateFolderRequest,
    ): BaseResponse<Any> {
        updateFolderUseCase.execute(UpdateFolderCommand(userId, folderId, body.name))

        return BaseResponse()
    }
}
