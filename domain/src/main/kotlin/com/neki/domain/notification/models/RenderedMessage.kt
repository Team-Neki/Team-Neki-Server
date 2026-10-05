package com.neki.domain.notification.models

/**
 * fileName       : RenderedMessage
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 렌더된 문구. actualTone 은 폴백이 일어났으면 배정 톤과 다르다
 */
data class RenderedMessage(
    val title: String,
    val body: String,
    val actualTone: MessageTone,
    val variableApplied: Boolean,
)
