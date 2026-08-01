package com.neki.user.infra.event

import com.neki.user.application.port.UserEventPublisherPort
import com.neki.user.event.UserEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class UserEventPublisher(private val applicationEventPublisher: ApplicationEventPublisher) :
    UserEventPublisherPort {
    override fun publish(event: UserEvent) = applicationEventPublisher.publishEvent(event)
}
