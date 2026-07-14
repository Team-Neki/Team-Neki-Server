package com.neki.photo.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.PutPhotoCommand
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.domain.entity.PhotoImage
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : PutPhotoUseCase
 * author         : koo
 * date           : 2026. 3. 15. 오후 10:56
 * description    :
 */
@UseCase
class PutPhotoUseCase(private val photoImageRepository: PhotoImageRepositoryPort) {

    /**
     * 개인 리소스만 변경하고, 하나의 기기만 로그인 가능하기 때문에 동시성 고려를 하지 않았습니다.
     * 스펙 변경에 따라 동시성 고려가 필요할 수 있습니다. (e.g. 여러 기기 로그인..)
     */
    @Transactional
    fun execute(command: PutPhotoCommand) {
        val photo: PhotoImage = (
            photoImageRepository.getOwnedPhoto(command.userId, command.photoId)
                ?: throw BusinessException(ResultCode.NOT_FOUND)
            )
        photo.update(command.memo, command.capturedAt)
    }
}
