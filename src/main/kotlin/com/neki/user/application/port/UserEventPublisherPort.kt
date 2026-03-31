package com.neki.user.application.port

interface UserEventPublisherPort {
    fun publish(event: Any)
}
