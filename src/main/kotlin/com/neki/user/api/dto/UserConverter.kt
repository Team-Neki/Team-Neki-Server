package com.neki.user.api.dto

import com.neki.common.properties.AppProperties
import com.neki.user.application.dto.UserCommand
import com.neki.user.application.dto.UserQuery
import com.neki.user.application.dto.UserResult
import org.springframework.stereotype.Component

/**
 * fileName       : UserConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User api layer converter
 */
object UserConverter {
    @Component
    class RequestConverter {
        fun toGetUserQuery(userId: Long) = UserQuery.GetUser(userId)

        fun toUpdateUserCommand(userId: Long, request: UserRequest.UpdateUser) =
            UserCommand.UpdateUserInfo(userId, request.name)

        fun toUpdateUserProfileImageCommand(userId: Long, request: UserRequest.UpdateUserProfileImage) =
            UserCommand.UpdateUserProfileImage(userId, request.mediaId)

        fun toDeleteUserCommand(userId: Long) = UserCommand.DeleteUser(userId)

        fun toLogoutCommand(userId: Long) = UserCommand.Logout(userId)
    }

    @Component
    class ResponseConverter(private val appProperties: AppProperties) {
        companion object {
            private const val IMAGE_URL_PATH = "/file/image/"
            private const val DEFAULT_PROFILE_KEY = "user-profiles/default_profile.png"
        }

        fun toGetUserResponse(result: UserResult.GetUser) = UserResponse.GetUser(
            userId = result.userId,
            name = result.name,
            email = result.email,
            profileImageUrl = toImageUrl(result.objectKey),
            providerType = result.providerType,
            agreeTerms = result.agreeTerms,
            marketingTerm = result.marketingTerm,
            pushAgreed = result.pushAgreed,
        )

        private fun toImageUrl(storageKey: String?): String =
            storageKey?.let { "${appProperties.server.url}$IMAGE_URL_PATH$storageKey" }
                ?: "${appProperties.server.url}${IMAGE_URL_PATH}$DEFAULT_PROFILE_KEY"
    }
}
