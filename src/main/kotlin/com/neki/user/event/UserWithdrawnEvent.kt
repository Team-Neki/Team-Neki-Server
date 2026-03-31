package com.neki.user.event

data class UserWithdrawnEvent(val userId: Long, val nickname: String, val activeUserCount: Long)
