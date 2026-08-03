package com.neki.user.dto

/**
 * fileName       : UserQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : User domain query
 */
object UserQuery {
    data class GetUser(val userId: Long)
}
