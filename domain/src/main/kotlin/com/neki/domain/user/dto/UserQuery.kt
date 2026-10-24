package com.neki.domain.user.dto

/**
 * fileName       : UserQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User domain query
 */
object UserQuery {
    data class GetGoogleAccount(val subject: String)

    data class GetActiveGoogleAccount(val userId: Long, val subject: String)

    data class GetUser(val userId: Long)
}
