package com.neki.notification.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "discord")
data class DiscordProperties(val webhookUrl: String)
