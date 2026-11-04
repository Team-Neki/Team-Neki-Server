package com.neki.domain.notification

import com.neki.domain.notification.models.MessageTone
import com.neki.domain.notification.models.MessageVariable
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.models.RenderedMessage
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** 문구는 docs/lld/notification-push/ 의 잡별 문서 표와 글자 그대로 같아야 한다 (공백, 느낌표 포함) */
class MessageRendererTest {

    private fun render(
        type: NotificationType,
        tone: MessageTone,
        variables: Map<MessageVariable, String?> = emptyMap(),
    ): RenderedMessage = MessageRenderer.render(type, tone, variables)

    private fun RenderedMessage.shouldRender(title: String, body: String, tone: MessageTone, applied: Boolean) {
        this.title shouldBe title
        this.body shouldBe body
        this.actualTone shouldBe tone
        this.variableApplied shouldBe applied
    }

    @Nested
    inner class VariableFreeTemplates {
        @Test
        fun `WEEKLY_REMINDER INFORMATIVE`() = render(NotificationType.WEEKLY_REMINDER, MessageTone.INFORMATIVE)
            .shouldRender("일주일 전 사진이 있어요", "네키에 저장한 네컷을 다시 확인해보세요.", MessageTone.INFORMATIVE, false)

        @Test
        fun `WEEKLY_REMINDER FRIENDLY`() = render(NotificationType.WEEKLY_REMINDER, MessageTone.FRIENDLY)
            .shouldRender("벌써 일주일 전 네컷이에요", "지난 사진을 네키에서 다시 꺼내보세요.", MessageTone.FRIENDLY, false)

        @Test
        fun `WEEKEND_EXPLORE INFORMATIVE`() = render(NotificationType.WEEKEND_EXPLORE, MessageTone.INFORMATIVE)
            .shouldRender("주말 전 포토부스 확인하기", "가까운 포토부스를 네키 지도에서 확인해보세요.", MessageTone.INFORMATIVE, false)

        @Test
        fun `WEEKEND_EXPLORE FRIENDLY`() = render(NotificationType.WEEKEND_EXPLORE, MessageTone.FRIENDLY)
            .shouldRender("이번 주말엔 어디서 찍을까요?", "약속 전에 근처 포토부스를 미리 찾아보세요.", MessageTone.FRIENDLY, false)

        @Test
        fun `WEEKEND_EXPLORE SUGGESTIVE`() = render(NotificationType.WEEKEND_EXPLORE, MessageTone.SUGGESTIVE)
            .shouldRender("약속 전에 미리 찾아보세요", "가까운 포토부스를 네키 지도에서 확인해보세요.", MessageTone.SUGGESTIVE, false)

        @Test
        fun `HOLIDAY_EXPLORE SUGGESTIVE`() = render(NotificationType.HOLIDAY_EXPLORE, MessageTone.SUGGESTIVE)
            .shouldRender("쉬는 날 가기 좋은 포토부스", "네키 지도에서 가까운 포토부스를 확인해보세요.", MessageTone.SUGGESTIVE, false)
    }

    @Nested
    inner class VariableSubstitution {
        @Test
        fun `WEEKLY_REMINDER SUGGESTIVE 는 최근 업로드 요일을 치환한다`() = render(
            NotificationType.WEEKLY_REMINDER,
            MessageTone.SUGGESTIVE,
            mapOf(
                MessageVariable.RECENT_UPLOAD_DAY to "지난 토요일",
            ),
        )
            .shouldRender("지난 토요일처럼 오늘도 남겨볼까요?", "오늘 찍은 사진도 네키에 정리해보세요.", MessageTone.SUGGESTIVE, true)

        @Test
        fun `HOLIDAY_EXPLORE INFORMATIVE 는 공휴일명을 치환한다`() = render(
            NotificationType.HOLIDAY_EXPLORE,
            MessageTone.INFORMATIVE,
            mapOf(
                MessageVariable.HOLIDAY_NAME to "어린이날",
            ),
        )
            .shouldRender("어린이날 포토부스 확인하기", "쉬는 날 방문할 포토부스를 네키 지도에서 확인해보세요.", MessageTone.INFORMATIVE, true)

        @Test
        fun `HOLIDAY_EXPLORE FRIENDLY 는 공휴일명을 치환한다 (본문 끝 느낌표)`() = render(
            NotificationType.HOLIDAY_EXPLORE,
            MessageTone.FRIENDLY,
            mapOf(MessageVariable.HOLIDAY_NAME to "어린이날"),
        )
            .shouldRender("어린이날에 약속 있으신가요?", "약속 전에 근처 포토부스를 미리 확인해보세요!", MessageTone.FRIENDLY, true)
    }

    @Nested
    inner class FallbackOnMissingVariable {
        private fun weeklyFallsBack(variables: Map<MessageVariable, String?>) =
            render(NotificationType.WEEKLY_REMINDER, MessageTone.SUGGESTIVE, variables)
                .shouldRender("일주일 전 사진이 있어요", "네키에 저장한 네컷을 다시 확인해보세요.", MessageTone.INFORMATIVE, false)

        private fun holidayFallsBack(tone: MessageTone, variables: Map<MessageVariable, String?>) =
            render(NotificationType.HOLIDAY_EXPLORE, tone, variables)
                .shouldRender("쉬는 날 가기 좋은 포토부스", "네키 지도에서 가까운 포토부스를 확인해보세요.", MessageTone.SUGGESTIVE, false)

        @Test
        fun `키가 없으면 폴백`() = weeklyFallsBack(emptyMap())

        @Test
        fun `값이 null 이면 폴백`() = weeklyFallsBack(mapOf(MessageVariable.RECENT_UPLOAD_DAY to null))

        @Test
        fun `값이 빈 문자열이면 폴백`() = weeklyFallsBack(mapOf(MessageVariable.RECENT_UPLOAD_DAY to ""))

        @Test
        fun `값이 공백이면 폴백`() = weeklyFallsBack(mapOf(MessageVariable.RECENT_UPLOAD_DAY to "   "))

        @Test
        fun `HOLIDAY_EXPLORE INFORMATIVE 는 공휴일명이 없으면 SUGGESTIVE 로 폴백`() =
            holidayFallsBack(MessageTone.INFORMATIVE, mapOf(MessageVariable.HOLIDAY_NAME to null))

        @Test
        fun `HOLIDAY_EXPLORE FRIENDLY 도 공휴일명이 없으면 SUGGESTIVE 로 폴백`() =
            holidayFallsBack(MessageTone.FRIENDLY, emptyMap())
    }

    @Test
    fun `WEEKEND_EXPLORE 는 변수가 들어와도 어느 톤에서도 쓰지 않는다`() {
        val populated = mapOf(MessageVariable.RECENT_UPLOAD_DAY to "지난 토요일", MessageVariable.HOLIDAY_NAME to "어린이날")
        for (tone in MessageTone.entries) {
            val result: RenderedMessage = render(NotificationType.WEEKEND_EXPLORE, tone, populated)
            result.variableApplied shouldBe false
            result.actualTone shouldBe tone
        }
    }
}
