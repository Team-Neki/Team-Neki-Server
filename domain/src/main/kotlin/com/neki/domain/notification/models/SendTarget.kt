package com.neki.domain.notification.models

/**
 * fileName       : SendTarget
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 발송 대상 1건. 동의자 페이지를 잡 조건으로 거르고 변수를 채운 결과
 */
data class SendTarget(
    val userId: Long,
    val deviceToken: String,
    val variables: Map<MessageVariable, String?> = emptyMap(),
)
