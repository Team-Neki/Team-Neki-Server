package com.neki.user.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.application.dto.UserQuery
import com.neki.user.application.dto.UserResult
import com.neki.user.application.port.MediaClientPort
import com.neki.user.application.port.NotificationClientPort
import com.neki.user.application.port.TermClientPort
import com.neki.user.application.port.UserRepositoryPort
import com.neki.user.entity.User

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

    fun execute(query: UserQuery.GetUser): UserResult.GetUser {
        val user: User = userRepository.findById(query.userId)
            ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

        val storageKey: String? = user.profileImageId?.let {
            mediaClient.getStorageKey(ownerId = user.id!!, mediaId = it)
        }

        val hasAgreedToAllRequired: Boolean = termClient.hasAgreedToAllRequired(user.id!!)

        val hasAgreedToMarketing = termClient.hasAgreedToMarketing(user.id!!)

        val pushAgreed: Boolean = notificationClient.isPushAgreed(user.id!!)

        return UserResult.GetUser(
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
