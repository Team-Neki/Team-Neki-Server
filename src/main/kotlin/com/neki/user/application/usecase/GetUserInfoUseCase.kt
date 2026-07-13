package com.neki.user.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.application.command.GetUserCommand
import com.neki.user.application.port.MediaClientPort
import com.neki.user.application.port.NotificationClientPort
import com.neki.user.application.port.TermClientPort
import com.neki.user.application.port.UserRepositoryPort
import com.neki.user.application.result.GetUserResult
import com.neki.user.domain.entity.User

/**
 * fileName       : GetMyInfoUseCase
 * author         : koo
 * date           : 2026. 1. 30. 오전 3:25
 * description    :
 */
@UseCase
class GetUserInfoUseCase(
    private val userRepository: UserRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val termClient: TermClientPort,
    private val notificationClient: NotificationClientPort,
) {

    fun execute(command: GetUserCommand): GetUserResult {
        val user: User = userRepository.findById(command.userId)
            ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

        val storageKey: String? = user.profileImageId?.let {
            mediaClient.getStorageKey(ownerId = user.id!!, mediaId = it)
        }

        val hasAgreedToAllRequired: Boolean = termClient.hasAgreedToAllRequired(user.id!!)

        val hasAgreedToMarketing = termClient.hasAgreedToMarketing(user.id!!)

        val pushAgreed: Boolean = notificationClient.isPushAgreed(user.id!!)

        return GetUserResult(
            userId = user.id!!,
            name = user.name!!,
            email = user.email,
            objectKey = storageKey,
            providerType = user.providerType,
            agreeTerms = hasAgreedToAllRequired,
            marketingTerm = hasAgreedToMarketing,
            pushAgreed = pushAgreed,
        )
    }
}
