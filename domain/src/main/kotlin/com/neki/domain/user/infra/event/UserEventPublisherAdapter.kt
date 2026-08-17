package com.neki.domain.user.infra.event

import com.neki.domain.user.external.UserEventPublisher
import com.neki.domain.user.models.UserEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class UserEventPublisherAdapter(private val applicationEventPublisher: ApplicationEventPublisher) :
    UserEventPublisher {
    override fun publish(event: UserEvent) = applicationEventPublisher.publishEvent(event)
}
