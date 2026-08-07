package com.neki.user.application

import com.neki.common.annotation.UseCase
import com.neki.user.application.dto.UserResult
import com.neki.user.client.MediaClient
import com.neki.user.client.NotificationClient
import com.neki.user.client.TermClient
import com.neki.user.dto.UserQuery
import com.neki.user.models.TermAgreementStatus
import com.neki.user.models.User
import com.neki.user.service.UserService

/**
 * fileName       : GetMyInfoUseCase
 * author         : koo
 * date           : 2026. 1. 30. 오전 3:25
 * description    :
 */
@UseCase
class GetUserInfoUseCase(
    private val userService: UserService,
    private val mediaClient: MediaClient,
    private val termClient: TermClient,
    private val notificationClient: NotificationClient,
) {

    fun execute(query: UserQuery.GetUser): UserResult.GetUser {
        val user: User = userService.getUser(query)

        val storageKey: String? = user.profileImageId?.let {
            mediaClient.getStorageKey(ownerId = user.id!!, mediaId = it)
        }

        val termAgreement: TermAgreementStatus = termClient.getAgreementStatus(user.id!!)

        val pushAgreed: Boolean = notificationClient.isPushAgreed(user.id!!)

        return UserResult.GetUser(
            userId = user.id!!,
            name = user.name!!,
            email = user.email,
            objectKey = storageKey,
            providerType = user.providerType,
            agreeTerms = termAgreement.requiredAgreed,
            marketingTerm = termAgreement.marketingAgreed,
            pushAgreed = pushAgreed,
        )
    }
}
