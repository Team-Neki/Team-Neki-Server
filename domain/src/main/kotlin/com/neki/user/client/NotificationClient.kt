package com.neki.user.client

interface NotificationClient {

    fun isPushAgreed(userId: Long): Boolean

    fun deleteFcmToken(userId: Long)
}
