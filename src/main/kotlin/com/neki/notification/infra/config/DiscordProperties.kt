package com.neki.notification.infra.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "discord")
class DiscordProperties(var webhookUrl: String = "")
