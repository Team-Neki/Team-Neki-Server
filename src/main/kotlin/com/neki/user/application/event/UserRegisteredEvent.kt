package com.neki.user.application.event

data class UserRegisteredEvent(val userId: Long, val nickname: String, val providerType: String, val platform: String, val activeUserCount: Long)
