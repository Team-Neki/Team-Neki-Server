package com.neki.domain.notification

import com.neki.domain.notification.models.MessageTone
import com.neki.domain.notification.models.MessageVariable
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.models.RenderedMessage

/**
 * fileName       : MessageRenderer
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 종류·톤별 문구 템플릿과 치환·폴백 규칙. 두 단계 when 이라 조합이 빠지면 컴파일이 막는다.
 *                  문구 표는 docs/lld/notification-push/<잡>.md
 */
object MessageRenderer {

    private data class Template(val title: String, val body: String, val requiredVariable: MessageVariable? = null)

    private fun templateFor(type: NotificationType, tone: MessageTone): Template = when (type) {
        NotificationType.WEEKLY_REMINDER -> when (tone) {
            MessageTone.INFORMATIVE -> Template(
                title = "일주일 전 사진이 있어요",
                body = "네키에 저장한 네컷을 다시 확인해보세요.",
            )
            MessageTone.FRIENDLY -> Template(
                title = "벌써 일주일 전 네컷이에요",
                body = "지난 사진을 네키에서 다시 꺼내보세요.",
            )
            MessageTone.SUGGESTIVE -> Template(
                title = "{최근 업로드 요일}처럼 오늘도 남겨볼까요?",
                body = "오늘 찍은 사진도 네키에 정리해보세요.",
                requiredVariable = MessageVariable.RECENT_UPLOAD_DAY,
            )
        }

        NotificationType.WEEKEND_EXPLORE -> when (tone) {
            MessageTone.INFORMATIVE -> Template(
                title = "주말 전 포토부스 확인하기",
                body = "가까운 포토부스를 네키 지도에서 확인해보세요.",
            )
            MessageTone.FRIENDLY -> Template(
                title = "이번 주말엔 어디서 찍을까요?",
                body = "약속 전에 근처 포토부스를 미리 찾아보세요.",
            )
            MessageTone.SUGGESTIVE -> Template(
                title = "약속 전에 미리 찾아보세요",
                body = "가까운 포토부스를 네키 지도에서 확인해보세요.",
            )
        }

        NotificationType.HOLIDAY_EXPLORE -> when (tone) {
            MessageTone.INFORMATIVE -> Template(
                title = "{공휴일명} 포토부스 확인하기",
                body = "쉬는 날 방문할 포토부스를 네키 지도에서 확인해보세요.",
                requiredVariable = MessageVariable.HOLIDAY_NAME,
            )
            MessageTone.FRIENDLY -> Template(
                title = "{공휴일명}에 약속 있으신가요?",
                body = "약속 전에 근처 포토부스를 미리 확인해보세요!",
                requiredVariable = MessageVariable.HOLIDAY_NAME,
            )
            MessageTone.SUGGESTIVE -> Template(
                title = "쉬는 날 가기 좋은 포토부스",
                body = "네키 지도에서 가까운 포토부스를 확인해보세요.",
            )
        }
    }

    /**
     * 1. 필요 변수가 없으면 그대로 2. 값이 있으면 치환 3. 값이 없거나 blank 면 폴백 톤 템플릿 (변수 불필요)
     */
    fun render(
        type: NotificationType,
        assignedTone: MessageTone,
        variables: Map<MessageVariable, String?>,
    ): RenderedMessage {
        val template: Template = templateFor(type, assignedTone)
        val required: MessageVariable = template.requiredVariable
            ?: return RenderedMessage(template.title, template.body, assignedTone, variableApplied = false)

        val value: String? = variables[required]
        if (!value.isNullOrBlank()) {
            return RenderedMessage(
                title = template.title.replace("{${required.token}}", value),
                body = template.body.replace("{${required.token}}", value),
                actualTone = assignedTone,
                variableApplied = true,
            )
        }

        val fallback: Template = templateFor(type, type.fallbackTone)
        return RenderedMessage(fallback.title, fallback.body, type.fallbackTone, variableApplied = false)
    }
}
