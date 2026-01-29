package com.yapp2app.user.api.converter

import com.yapp2app.common.properties.AppProperties
import com.yapp2app.user.api.dto.GetUserResponse
import com.yapp2app.user.application.result.GetUserResult
import org.springframework.stereotype.Component

/**
 * fileName       : UserResultConverter
 * author         : koo
 * date           : 2026. 1. 30. 오전 3:31
 * description    :
 */
@Component
class UserResultConverter(private val appProperties: AppProperties) {

    companion object {
        private const val IMAGE_URL_PATH = "/file/image/"
        private const val DEFAULT_PROFILE_KEY = "user-profiles/default_profile.png"
    }

    fun toGetUserResponse(result: GetUserResult) = GetUserResponse(
        userId = result.userId,
        name = result.name,
        email = result.email,
        profileImageUrl = toImageUrl(result.objectKey),
        providerType = result.providerType,
    )

    private fun toImageUrl(storageKey: String?): String =
        storageKey?.let { "${appProperties.server.url}$IMAGE_URL_PATH$storageKey" }
            ?: "${appProperties.server.url}${IMAGE_URL_PATH}$DEFAULT_PROFILE_KEY"
}
