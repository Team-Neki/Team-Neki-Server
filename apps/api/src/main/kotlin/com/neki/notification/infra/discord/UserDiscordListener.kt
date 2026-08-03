package com.neki.notification.infra.discord

import com.neki.config.discord.DiscordProperties
import com.neki.user.models.UserEvent
import com.neki.user.models.UserRegisteredEvent
import com.neki.user.models.UserWithdrawnEvent
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
class UserDiscordListener(
    private val discordProperties: DiscordProperties,
    private val restClient: RestClient,
    private val environment: Environment,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("discordNotificationExecutor")
    @EventListener
    fun handle(event: UserEvent) {
        if (discordProperties.webhookUrl.isBlank()) return

        val profile: String = environment.activeProfiles.firstOrNull() ?: "unknown"

        val (title, color) = when (event) {
            is UserRegisteredEvent -> "Neki 신규 사용자 가입 알림" to 5763719
            is UserWithdrawnEvent -> "Neki 사용자 탈퇴 알림" to 15548997
        }

        val fields = mutableListOf(
            field("환경", profile),
            field("ID", event.userId),
            field("닉네임", event.nickname),
        )

        if (event is UserRegisteredEvent) {
            fields += field("로그인 유형", event.providerType)
            fields += field("플랫폼", event.platform)
        }

        // 필드 순서로 인해 마지막에 추가
        fields += field("누적 가입자 (탈퇴 제외)", event.activeUserCount)

        sendEmbed(title, color, fields)
    }

    private fun field(name: String, value: Any): Map<String, Any> =
        mapOf("name" to name, "value" to value.toString(), "inline" to false)

    private fun sendEmbed(title: String, color: Int, fields: List<Map<String, Any>>) {
        runCatching {
            val body = mapOf("embeds" to listOf(mapOf("title" to title, "color" to color, "fields" to fields)))
            restClient.post()
                .uri(discordProperties.webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
        }.onFailure { log.warn("Discord notification failed: {}", it.message) }
    }
}
