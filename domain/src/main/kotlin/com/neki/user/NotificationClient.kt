package com.neki.user

interface NotificationClient {

    fun isPushAgreed(userId: Long): Boolean

    fun deleteFcmToken(userId: Long)
}
