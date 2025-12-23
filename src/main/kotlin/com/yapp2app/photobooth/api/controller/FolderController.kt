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
@Tag(name = "folder")
@RestController
@RequestMapping("/api/folders")
class FolderController(
    private val createFolderUseCase: CreateFolderUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
) {

    @PostMapping
    fun createFolder(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody body: CreateFolderRequest,
    ): BaseResponse<Any> {
        createFolderUseCase.execute(CreateFolderCommand(userId, body.name))

        return BaseResponse()
    }

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

    @DeleteMapping("/{folderId}")
    fun deleteFolder(@AuthenticationPrincipal userId: Long, @PathVariable folderId: Long): BaseResponse<Any> {
        deleteFolderUseCase.execute(DeleteFolderCommand(userId, folderId))

        return BaseResponse()
    }

    @DeleteMapping
    fun deleteFolders(
        @AuthenticationPrincipal userId: Long,
        @RequestBody body: DeleteFoldersRequest,
    ): BaseResponse<Any> {
        deleteFolderUseCase.execute(DeleteFoldersCommand(userId, body.folderIds))

        return BaseResponse()
    }

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
