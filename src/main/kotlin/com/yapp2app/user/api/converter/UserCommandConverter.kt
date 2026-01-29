package com.yapp2app.user.api.converter

import com.yapp2app.user.api.dto.UpdateUserRequest
import com.yapp2app.user.application.command.UpdateUserCommand
import org.springframework.stereotype.Component

/**
 * fileName       : UserCommandConverter
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:49
 * description    :
 */
@Component
class UserCommandConverter {

    fun toUpdateUserCommand(userId: Long, request: UpdateUserRequest) =
        UpdateUserCommand(userId, request.mediaId, request.name)
}
