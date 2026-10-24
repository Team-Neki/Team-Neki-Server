package com.neki.domain.user.dto

/**
 * fileName       : UserCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User domain command
 */
object UserCommand {
    data class RegisterAdminAccount(val identityKey: String, val email: String, val name: String?)

    data class AdminAccountAccess(val userId: Long, val identityKey: String)

    data class UpdateUserInfo(val userId: Long, val name: String)

    data class UpdateUserProfileImage(val userId: Long, val mediaId: Long?)

    data class DeleteUser(val userId: Long)

    data class Logout(val userId: Long)
}
