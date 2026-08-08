package com.neki.domain.user.external

import com.neki.domain.user.models.UserEvent

interface UserEventPublisher {
    fun publish(event: UserEvent)
}
