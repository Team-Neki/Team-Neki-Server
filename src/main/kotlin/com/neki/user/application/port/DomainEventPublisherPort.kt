package com.neki.user.application.port

interface DomainEventPublisherPort {
    fun publish(event: Any)
}
