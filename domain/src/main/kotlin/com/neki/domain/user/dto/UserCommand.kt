package com.neki.domain.user.dto

import com.neki.domain.user.models.AdminIdentity

/**
 * fileName       : UserCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User domain command
 */
object UserCommand {
    data class RegisterAdminAccount(val identity: AdminIdentity)

    data class UpdateUserInfo(val userId: Long, val name: String)

    data class UpdateUserProfileImage(val userId: Long, val mediaId: Long?)

    data class DeleteUser(val userId: Long)

    data class Logout(val userId: Long)
}
