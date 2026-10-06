package com.neki.domain.notification

import com.neki.domain.notification.models.MessageTone
import com.neki.domain.notification.models.MessageVariable
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.models.SendDecision
import com.neki.domain.notification.models.SendTarget
import com.neki.domain.notification.models.SkipReason
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** 중복이면 Skip, 아니면 톤 배정 + 렌더. 동의는 읽기 쿼리가 보므로 여기서 다루지 않는다 */
class NotificationProcessorTest {

    /** epochDay 20718 은 3 의 배수라 이 날의 톤은 userId % 3 과 같다 */
    private val businessDate = LocalDate.of(2026, 9, 22)

    private fun target(userId: Long, variables: Map<MessageVariable, String?> = emptyMap()) =
        SendTarget(userId = userId, deviceToken = "token-$userId", variables = variables)

    @Test
    fun `이미 발송됐으면 ALREADY_SENT 로 스킵`() {
        NotificationProcessor.decide(
            target(1L),
            NotificationType.WEEKEND_EXPLORE,
            alreadySent = true,
            businessDate,
        ) shouldBe
            SendDecision.Skip(SkipReason.ALREADY_SENT)
    }

    @Test
    fun `미중복이고 변수 없는 종류면 기본 문구로 발송`() {
        val send = NotificationProcessor.decide(
            target(0L),
            NotificationType.WEEKEND_EXPLORE,
            alreadySent = false,
            businessDate,
        )
            .shouldBeInstanceOf<SendDecision.Send>()
        send.message.actualTone shouldBe MessageTone.INFORMATIVE
        send.message.title shouldBe "주말 전 포토부스 확인하기"
        send.message.variableApplied shouldBe false
    }

    @Test
    fun `톤은 businessDate 를 반영한다`() {
        val nextDay = businessDate.plusDays(1) // userId 2 + epochDay 20719 -> 0 -> INFORMATIVE
        val send = NotificationProcessor.decide(
            target(2L),
            NotificationType.WEEKEND_EXPLORE,
            alreadySent = false,
            nextDay,
        )
            .shouldBeInstanceOf<SendDecision.Send>()
        send.message.actualTone shouldBe ToneAssignmentPolicy.assign(2L, nextDay)
        send.message.actualTone shouldBe MessageTone.INFORMATIVE
    }

    @Test
    fun `변수값이 있으면 개인화 문구로 발송`() {
        val send = NotificationProcessor.decide(
            target(2L, mapOf(MessageVariable.RECENT_UPLOAD_DAY to "지난 토요일")),
            NotificationType.WEEKLY_REMINDER,
            alreadySent = false,
            businessDate,
        ).shouldBeInstanceOf<SendDecision.Send>()
        send.message.actualTone shouldBe MessageTone.SUGGESTIVE
        send.message.variableApplied shouldBe true
        send.message.title shouldBe "지난 토요일처럼 오늘도 남겨볼까요?"
    }

    @Test
    fun `변수값이 없으면 폴백 톤 기본 문구로 발송`() {
        val send = NotificationProcessor.decide(
            target(2L),
            NotificationType.WEEKLY_REMINDER,
            alreadySent = false,
            businessDate,
        )
            .shouldBeInstanceOf<SendDecision.Send>()
        send.message.actualTone shouldBe MessageTone.INFORMATIVE
        send.message.variableApplied shouldBe false
        send.message.title shouldBe "일주일 전 사진이 있어요"
    }
}
