package com.neki.user.external

import com.neki.user.models.UserEvent

interface UserEventPublisher {
    fun publish(event: UserEvent)
}
