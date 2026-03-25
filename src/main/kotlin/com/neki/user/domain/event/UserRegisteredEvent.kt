package com.neki.user.domain.event

data class UserRegisteredEvent(val userId: Long, val nickname: String, val providerType: String, val platform: String)
