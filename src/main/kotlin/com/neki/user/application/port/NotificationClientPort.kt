package com.neki.user.application.port

interface NotificationClientPort {

    fun isPushAgreed(userId: Long): Boolean

    fun deleteFcmToken(userId: Long)
}
