package com.neki.domain.user.dto

/**
 * fileName       : UserQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User domain query
 */
object UserQuery {
    data class GetAdminAccount(val identityKey: String)

    data class GetActiveAdminAccount(val userId: Long, val identityKey: String)

    data class GetUser(val userId: Long)
}
