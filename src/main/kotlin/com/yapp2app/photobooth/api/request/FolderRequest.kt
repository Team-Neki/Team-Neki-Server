package com.yapp2app.photobooth.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * fileName       : FolderRequest
 * author         : koo
 * date           : 2025. 12. 23. 오후 10:27
 * description    : 폴더 관련 요청 body
 */
data class CreateFolderRequest(@field:NotBlank val name: String)

data class DeleteFoldersRequest(@field:NotEmpty val folderIds: List<Long>)

data class UpdateFolderRequest(@field:NotBlank val name: String)
