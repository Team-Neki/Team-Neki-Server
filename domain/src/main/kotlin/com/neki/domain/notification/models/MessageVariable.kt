package com.neki.domain.notification.models

/**
 * fileName       : MessageVariable
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 문구 치환 변수. 템플릿 안에서는 {토큰} 으로 쓴다
 */
enum class MessageVariable(val token: String) {
    RECENT_UPLOAD_DAY("최근 업로드 요일"),
    HOLIDAY_NAME("공휴일명"),
}
