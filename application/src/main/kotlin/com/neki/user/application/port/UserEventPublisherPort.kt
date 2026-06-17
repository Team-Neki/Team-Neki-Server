package com.neki.user.application.port

import com.neki.user.event.UserEvent

interface UserEventPublisherPort {
    fun publish(event: UserEvent)
}
