package com.neki.user.infra.event

import com.neki.user.application.port.UserEventPublisherPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class UserEventPublisher(private val applicationEventPublisher: ApplicationEventPublisher) :
    UserEventPublisherPort {
    override fun publish(event: Any) = applicationEventPublisher.publishEvent(event)
}
