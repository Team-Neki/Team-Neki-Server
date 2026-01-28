package com.yapp2app.photo.api.converter

import com.yapp2app.common.properties.AppProperties
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
class FolderResultConverter(private val appProperties: AppProperties) {

    companion object {
        private const val IMAGE_URL_PATH = "/file/image/"
    }

    fun toGetAllFoldersResponse(result: GetFoldersResult): GetAllFolderResponse = GetAllFolderResponse(
        items = result.items.map {
            GetAllFolderResponse.FolderInfo(
                it.folderId,
                it.name,
                latestImageUrl = it.imageObjectKey?.let { key -> toImageUrl(key) },
                totalCount = it.count,
            )
        },

    )

    fun toCreateFolderResponse(result: CreateFolderResult): CreateFolderResponse = CreateFolderResponse(result.folderId)

    private fun toImageUrl(objectKey: String): String = "${appProperties.server.url}$IMAGE_URL_PATH$objectKey"
}
