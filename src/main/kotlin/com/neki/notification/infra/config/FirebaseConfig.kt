package com.neki.notification.infra.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

/**
 * fileName       : FirebaseConfig
 * author         : darren
 * date           : 2026. 6. 14
 * description    : FCM 푸시 발송을 위한 Firebase Admin SDK 초기화.
 *                  서비스 계정 키가 존재하는 위치(firebase.credentials-location)에서만 활성화된다.
 *                  - local : classpath:firebase-service-account.json (기본값)
 *                  - k8s   : file:/etc/firebase/firebase-service-account.json (secret 볼륨 마운트 경로)
 *                  키가 없는 환경(test/CI 등)에서는 빈이 등록되지 않는다.
 */
@Configuration
@ConditionalOnResource(resources = ["\${firebase.credentials-location:classpath:firebase-service-account.json}"])
class FirebaseConfig(
    @Value("\${firebase.credentials-location:classpath:firebase-service-account.json}")
    private val credentialsLocation: Resource,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun firebaseApp(): FirebaseApp {
        credentialsLocation.inputStream.use { credentialStream ->
            val options: FirebaseOptions = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialStream))
                .build()

            return if (FirebaseApp.getApps().isEmpty()) {
                log.info("Initializing FirebaseApp for FCM push (location={})", credentialsLocation.description)
                FirebaseApp.initializeApp(options)
            } else {
                FirebaseApp.getInstance()
            }
        }
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)
}
