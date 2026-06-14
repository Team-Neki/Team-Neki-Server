package com.neki.notification.infra.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.notification.application.port.PushNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * fileName       : FcmPushNotificationAdapter
 * author         : darren
 * date           : 2026. 6. 14
 * description    : Firebase Admin SDK(FCM)를 이용한 푸시 발송 어댑터.
 *                  서비스 계정 키가 없어 FirebaseMessaging 빈이 없으면 호출 시점에 예외를 던진다.
 */
@Component
class FcmPushNotificationAdapter(private val firebaseMessagingProvider: ObjectProvider<FirebaseMessaging>) :
    PushNotificationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(token: String, title: String, body: String, link: String?): String {
        val firebaseMessaging: FirebaseMessaging = firebaseMessagingProvider.ifAvailable
            ?: throw BusinessException(ResultCode.PUSH_NOT_CONFIGURED)

        val message: Message = Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build(),
            )
            .apply { link?.let { putData("link", it) } }
            .build()

        return try {
            val messageId: String = firebaseMessaging.send(message)
            log.info("FCM push sent: messageId={}", messageId)
            messageId
        } catch (e: FirebaseMessagingException) {
            log.error("FCM push failed: token={}, errorCode={}", token, e.messagingErrorCode, e)
            throw BusinessException(ResultCode.PUSH_SEND_FAILED)
        }
    }
}
