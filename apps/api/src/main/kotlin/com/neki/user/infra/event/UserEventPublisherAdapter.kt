package com.neki.user.infra.event

import com.neki.user.UserEventPublisher
import com.neki.user.models.UserEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class UserEventPublisherAdapter(private val applicationEventPublisher: ApplicationEventPublisher) :
    UserEventPublisher {
    override fun publish(event: UserEvent) = applicationEventPublisher.publishEvent(event)
}
