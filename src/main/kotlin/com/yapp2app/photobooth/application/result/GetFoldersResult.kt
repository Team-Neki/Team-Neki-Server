package com.yapp2app.photobooth.application.result

/**
 * fileName       : GetFoldersResult
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:10
 * description    : Folder usecase 관련 result (TODO : 파일 추가에 따른 파일명 변경)
 */
data class GetFoldersResult(val items: List<FolderInfo>) {
    data class FolderInfo(val folderId: Long, val name: String)
}
