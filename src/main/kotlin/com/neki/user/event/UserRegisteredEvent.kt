package com.neki.user.event

data class UserRegisteredEvent(
    val userId: Long,
    val nickname: String,
    val providerType: String,
    val platform: String,
    val activeUserCount: Long,
)
