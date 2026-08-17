package com.neki.domain.notification.infra.fcm

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.ApsAlert
import com.google.firebase.messaging.FcmOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.notification.external.PushNotificationSender
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
    PushNotificationSender {

    private val log = LoggerFactory.getLogger(javaClass)

    private companion object {
        /** 콘솔 Delivery 리포트에서 서버 발송분을 식별하기 위한 분석 라벨 */
        const val ANALYTICS_LABEL = "server_push"
    }

    override fun send(token: String, title: String, body: String, link: String?): String {
        val firebaseMessaging: FirebaseMessaging = firebaseMessagingProvider.ifAvailable
            ?: throw BusinessException(ResultCode.PUSH_NOT_CONFIGURED)

        val message: Message = Message.builder()
            .setToken(token)
            .putData("title", title)
            .putData("body", body)
            .apply { link?.let { putData("link", it) } }
            .setAndroidConfig(androidConfig(title, body))
            .setApnsConfig(apnsConfig(title, body))
            .setFcmOptions(FcmOptions.builder().setAnalyticsLabel(ANALYTICS_LABEL).build())
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

    /** Android: 백그라운드/종료 상태에서도 시스템이 알림을 자동 표시하도록 notification 페이로드를 함께 보낸다. */
    private fun androidConfig(title: String, body: String): AndroidConfig = AndroidConfig.builder()
        .setPriority(AndroidConfig.Priority.HIGH)
        .setNotification(
            AndroidNotification.builder()
                .setTitle(title)
                .setBody(body)
                .build(),
        )
        .build()

    /** iOS: aps.alert 가 있어야 시스템이 알림을 표시한다. mutable-content 로 표시 전 가공(NSE)을 허용한다. */
    private fun apnsConfig(title: String, body: String): ApnsConfig = ApnsConfig.builder()
        .putHeader("apns-priority", "10")
        .setAps(
            Aps.builder()
                .setAlert(
                    ApsAlert.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build(),
                )
                .setSound("default")
                .setMutableContent(true)
                .build(),
        )
        .build()
}
