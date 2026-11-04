package com.neki.domain.notification.models

/**
 * fileName       : NotificationType
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 배치가 보내는 알림 종류. 잡 하나가 종류 하나를 맡는다
 */
enum class NotificationType {
    WEEKLY_REMINDER,
    WEEKEND_EXPLORE,
    HOLIDAY_EXPLORE,
    ;

    /** 필요 변수가 비었을 때 내려가는 톤. 폴백 톤의 템플릿은 변수를 쓰지 않는다 */
    val fallbackTone: MessageTone
        get() = when (this) {
            WEEKLY_REMINDER -> MessageTone.INFORMATIVE
            WEEKEND_EXPLORE -> MessageTone.INFORMATIVE
            HOLIDAY_EXPLORE -> MessageTone.SUGGESTIVE
        }
}
