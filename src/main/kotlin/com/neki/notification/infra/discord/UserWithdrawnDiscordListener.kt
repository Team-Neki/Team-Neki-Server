package com.neki.notification.infra.discord

import com.neki.notification.properties.DiscordProperties
import com.neki.user.event.UserWithdrawnEvent
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.client.RestClient

@Profile("!test")
@Component
class UserWithdrawnDiscordListener(
    private val discordProperties: DiscordProperties,
    private val restClient: RestClient,
    private val environment: Environment,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("discordNotificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onUserWithdrawn(event: UserWithdrawnEvent) {
        if (discordProperties.webhookUrl.isBlank()) return

        val profile: String = environment.activeProfiles.firstOrNull() ?: "unknown"

        runCatching {
            val body: Map<String, Any> = mapOf(
                "embeds" to listOf(
                    mapOf(
                        "title" to "Neki 사용자 탈퇴 알림",
                        "color" to 15548997, // Discord red
                        "fields" to listOf(
                            mapOf("name" to "환경", "value" to profile, "inline" to false),
                            mapOf("name" to "ID", "value" to event.userId.toString(), "inline" to false),
                            mapOf("name" to "닉네임", "value" to event.nickname, "inline" to false),
                            mapOf("name" to "누적 가입자 (탈퇴 제외)", "value" to event.activeUserCount, "inline" to false),
                        ),
                    ),
                ),
            )
            restClient.post()
                .uri(discordProperties.webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
        }.onFailure { log.warn("Discord notification failed: {}", it.message) }
    }
}
