package com.neki.api.user.application

import com.neki.api.user.application.dto.UserResult
import com.neki.core.annotation.UseCase
import com.neki.domain.user.client.MediaClient
import com.neki.domain.user.client.NotificationClient
import com.neki.domain.user.client.TermClient
import com.neki.domain.user.dto.UserQuery
import com.neki.domain.user.models.TermAgreementStatus
import com.neki.domain.user.models.User
import com.neki.domain.user.service.UserService

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
