package com.neki.user.api.converter

import com.neki.user.api.dto.UpdateUserProfileImageRequest
import com.neki.user.api.dto.UpdateUserRequest
import com.neki.user.application.dto.UserCommand
import com.neki.user.application.dto.UserQuery
import org.springframework.stereotype.Component

/**
 * fileName       : UserCommandConverter
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:49
 * description    :
 */
@Component
class UserCommandConverter {

    fun toGetUserQuery(userId: Long) = UserQuery.GetUser(userId)

    fun toUpdateUserCommand(userId: Long, request: UpdateUserRequest) = UserCommand.UpdateUserInfo(userId, request.name)

    fun toUpdateUserProfileImageCommand(userId: Long, request: UpdateUserProfileImageRequest) =
        UserCommand.UpdateUserProfileImage(userId, request.mediaId)

    fun toDeleteUserCommand(userId: Long) = UserCommand.DeleteUser(userId)

    fun toLogoutCommand(userId: Long) = UserCommand.Logout(userId)
}
