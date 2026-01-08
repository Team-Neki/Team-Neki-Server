package com.yapp2app.photo.api.converter

import com.yapp2app.photo.api.dto.CreateFolderResponse
import com.yapp2app.photo.api.dto.GetAllFolderResponse
import com.yapp2app.photo.application.result.CreateFolderResult
import com.yapp2app.photo.application.result.GetFoldersResult
import org.springframework.stereotype.Component

/**
 * fileName       : FolderDtoMapper
 * author         : koo
 * date           : 2025. 12. 28. 오후 9:41
 * description    :
 */
@Component
class FolderResultConverter {

    fun toGetAllFoldersResponse(result: GetFoldersResult): GetAllFolderResponse = GetAllFolderResponse(
        result.items.map {
            GetAllFolderResponse.FolderInfo(
                it.folderId,
                it.name,
            )
        },
    )

    fun toCreateFolderResponse(result: CreateFolderResult): CreateFolderResponse = CreateFolderResponse(result.folderId)
}
