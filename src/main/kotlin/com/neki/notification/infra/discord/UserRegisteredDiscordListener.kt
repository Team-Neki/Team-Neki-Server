package com.neki.notification.infra.discord

import com.neki.notification.properties.DiscordProperties
import com.neki.user.event.UserRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Profile("!test")
@Component
class UserRegisteredDiscordListener(
    private val discordProperties: DiscordProperties,
    private val restClient: RestClient,
    private val environment: Environment,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("discordNotificationExecutor")
    @EventListener
    fun onUserRegistered(event: UserRegisteredEvent) {
        if (discordProperties.webhookUrl.isBlank()) return

        val profile: String = environment.activeProfiles.firstOrNull() ?: "unknown"

        runCatching {
            val body: Map<String, Any> = mapOf(
                "embeds" to listOf(
                    mapOf(
                        "title" to "Neki 신규 사용자 가입 알림",
                        "color" to 5763719, // Discord green
                        "fields" to listOf(
                            mapOf("name" to "환경", "value" to profile, "inline" to false),
                            mapOf("name" to "ID", "value" to event.userId.toString(), "inline" to false),
                            mapOf("name" to "닉네임", "value" to event.nickname, "inline" to false),
                            mapOf("name" to "로그인 유형", "value" to event.providerType, "inline" to false),
                            mapOf("name" to "플랫폼", "value" to event.platform, "inline" to false),
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
