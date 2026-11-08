package com.neki.domain.user.dto

/**
 * fileName       : UserQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User domain query
 */
object UserQuery {
    data class GetAdminAccount(val email: String)

    data class GetActiveAdminAccount(val userId: Long)

    data class GetUser(val userId: Long)
}
