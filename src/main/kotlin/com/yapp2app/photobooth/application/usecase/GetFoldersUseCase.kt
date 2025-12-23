package com.yapp2app.photobooth.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.photobooth.application.command.GetFoldersCommand
import com.yapp2app.photobooth.application.port.FolderRepositoryPort
import com.yapp2app.photobooth.application.result.GetFoldersResult
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetFoldersUseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 8:04
 * description    : 폴더 목록 조회 usecase (TODO : 필요에 따라 Paging 추가)
 */
@UseCase
class GetFoldersUseCase(private val folderRepository: FolderRepositoryPort) {

    @Transactional(readOnly = true)
    fun execute(command: GetFoldersCommand): GetFoldersResult {
        val folders = folderRepository.findAll(command.userId)
            .map { GetFoldersResult.FolderInfo(it.id!!, it.name) }
            .toList()

        return GetFoldersResult(folders)
    }
}
