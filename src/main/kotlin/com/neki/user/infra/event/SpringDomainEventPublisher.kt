package com.neki.user.infra.event

import com.neki.user.application.port.DomainEventPublisherPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringDomainEventPublisher(private val applicationEventPublisher: ApplicationEventPublisher) :
    DomainEventPublisherPort {
    override fun publish(event: Any) = applicationEventPublisher.publishEvent(event)
}
