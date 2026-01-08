package com.yapp2app.photo.api.dto

/**
 * fileName       : PhotoBoothResponse
 * author         : koo
 * date           : 2025. 12. 23. 오후 6:50
 * description    : Folder aggregate에 대한 응답 (TODO : data class 추가에 따라 파일명 변경)
 */
data class CreateFolderResponse(val folderId: Long)

data class GetAllFolderResponse(val items: List<FolderInfo>) {
    data class FolderInfo(val folderId: Long, val name: String)
}
