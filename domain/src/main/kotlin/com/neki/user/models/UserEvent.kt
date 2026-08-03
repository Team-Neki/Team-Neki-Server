package com.neki.user.models

sealed interface UserEvent {
    val userId: Long
    val nickname: String
    val activeUserCount: Long
}

data class UserRegisteredEvent(
    override val userId: Long,
    override val nickname: String,
    val providerType: String,
    val platform: String,
    override val activeUserCount: Long,
) : UserEvent

data class UserWithdrawnEvent(
    override val userId: Long,
    override val nickname: String,
    override val activeUserCount: Long,
) : UserEvent
