package com.neki.user.api.converter

import com.neki.user.api.dto.UpdateUserProfileImageRequest
import com.neki.user.api.dto.UpdateUserRequest
import com.neki.user.application.command.DeleteUserCommand
import com.neki.user.application.command.GetUserCommand
import com.neki.user.application.command.UpdateUserInfoCommand
import com.neki.user.application.command.UpdateUserProfileImageCommand
import org.springframework.stereotype.Component

/**
 * fileName       : UserCommandConverter
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:49
 * description    :
 */
@Component
class UserCommandConverter {

    fun toGetUserCommand(userId: Long) = GetUserCommand(userId)

    fun toUpdateUserCommand(userId: Long, request: UpdateUserRequest) = UpdateUserInfoCommand(userId, request.name)

    fun toUpdateUserProfileImageCommand(userId: Long, request: UpdateUserProfileImageRequest) =
        UpdateUserProfileImageCommand(userId, request.mediaId)

    fun toDeleteUserCommand(userId: Long) = DeleteUserCommand(userId)
}
