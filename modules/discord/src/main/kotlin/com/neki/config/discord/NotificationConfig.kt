package com.neki.config.discord

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class NotificationConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean(name = ["discordNotificationExecutor"])
    fun discordNotificationExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 2
        maxPoolSize = 10
        queueCapacity = 50
        setThreadNamePrefix("discord-")
        setRejectedExecutionHandler { _, executor ->
            log.error(
                "Discord notification dropped: queue={}/{}, active={}/{}",
                executor.queue.size,
                executor.queue.size + executor.queue.remainingCapacity(),
                executor.activeCount,
                executor.maximumPoolSize,
            )
        }
        initialize()
    }
}
