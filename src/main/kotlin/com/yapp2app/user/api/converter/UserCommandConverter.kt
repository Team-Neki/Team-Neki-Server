package com.yapp2app.user.api.converter

import com.yapp2app.user.api.dto.UpdateUserProfileImageRequest
import com.yapp2app.user.api.dto.UpdateUserRequest
import com.yapp2app.user.application.command.DeleteUserCommand
import com.yapp2app.user.application.command.GetUserCommand
import com.yapp2app.user.application.command.UpdateUserInfoCommand
import com.yapp2app.user.application.command.UpdateUserProfileImageCommand
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
        UpdateUserProfileImageCommand(userId, request.mediaId!!)

    fun toDeleteUserCommand(userId: Long) = DeleteUserCommand(userId)
}
