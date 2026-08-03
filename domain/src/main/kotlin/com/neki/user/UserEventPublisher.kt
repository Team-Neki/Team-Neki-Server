package com.neki.user

import com.neki.user.models.UserEvent

interface UserEventPublisher {
    fun publish(event: UserEvent)
}
