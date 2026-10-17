package com.neki.domain.notification.models

/**
 * fileName       : SendDecision
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 대상 1건의 처리 판정. Send 는 렌더된 문구를 들고 발송으로, Skip 은 사유와 함께 제외
 */
sealed interface SendDecision {
    data class Send(val message: RenderedMessage) : SendDecision

    data class Skip(val reason: SkipReason) : SendDecision
}

/** 동의 필터는 읽기 쿼리(push_agreed = true)가 맡으므로 여기엔 중복 사유만 있다 */
enum class SkipReason {
    ALREADY_SENT,
}
