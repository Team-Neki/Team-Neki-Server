package com.neki.photo.api.converter

import com.neki.common.properties.AppProperties
import com.neki.photo.api.dto.CreateFolderResponse
import com.neki.photo.api.dto.GetAllFolderResponse
import com.neki.photo.application.dto.FolderResult
import org.springframework.stereotype.Component

/**
 * fileName       : FolderDtoMapper
 * author         : koo
 * date           : 2025. 12. 28. 오후 9:41
 * description    :
 */
@Component
class FolderResultConverter(private val appProperties: AppProperties) {

    companion object {
        private const val IMAGE_URL_PATH = "/file/image/"
    }

    fun toGetAllFoldersResponse(result: FolderResult.GetFolders): GetAllFolderResponse = GetAllFolderResponse(
        items = result.items.map {
            GetAllFolderResponse.FolderInfo(
                it.folderId,
                it.name,
                latestImageUrl = it.storageKey?.let { key -> toImageUrl(key) },
                totalCount = it.count,
            )
        },

    )

    fun toCreateFolderResponse(result: FolderResult.CreateFolder): CreateFolderResponse =
        CreateFolderResponse(result.folderId)

    private fun toImageUrl(objectKey: String): String = "${appProperties.server.url}$IMAGE_URL_PATH$objectKey"
}
