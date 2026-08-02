package com.neki.config.discord

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile

@Profile("!test")
@ConfigurationProperties(prefix = "discord")
data class DiscordProperties(val webhookUrl: String)
