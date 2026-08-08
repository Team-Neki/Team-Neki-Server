package com.neki.api.photo.application

import com.neki.core.annotation.UseCase
import com.neki.domain.photo.dto.PhotoImageCommand
import com.neki.domain.photo.service.PhotoService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdatePhotoUseCase
 * author         : koo
 * date           : 2026. 1. 9. 오후 3:53
 * description    : 사진 업데이트 UseCase
 */
@Deprecated(message = "PUT API 변경 후 제거")
@UseCase
class UpdatePhotoUseCase(private val photoService: PhotoService) {

    /**
     * 개인 리소스만 변경하고, 하나의 기기만 로그인 가능하기 때문에 동시성 고려를 하지 않았습니다.
     * 스펙 변경에 따라 동시성 고려가 필요할 수 있습니다. (e.g. 여러 기기 로그인..)
     */
    @Transactional
    fun execute(command: PhotoImageCommand.UpdatePhoto) = photoService.updatePhotoMemo(command)
}
