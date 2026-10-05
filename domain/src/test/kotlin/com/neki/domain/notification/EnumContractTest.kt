package com.neki.domain.notification

import com.neki.domain.notification.models.MessageTone
import com.neki.domain.notification.models.MessageVariable
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.models.SendTarget
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** MessageTone 선언 순서(톤 배정이 index 로 쓴다), 종류별 폴백 톤, 변수 토큰, SendTarget 기본값 */
class EnumContractTest {

    @Test
    fun `MessageTone 순서는 INFORMATIVE, FRIENDLY, SUGGESTIVE 로 고정이다`() {
        MessageTone.entries shouldContainExactly
            listOf(MessageTone.INFORMATIVE, MessageTone.FRIENDLY, MessageTone.SUGGESTIVE)
    }

    @Test
    fun `종류별 폴백 톤`() {
        NotificationType.WEEKLY_REMINDER.fallbackTone shouldBe MessageTone.INFORMATIVE
        NotificationType.WEEKEND_EXPLORE.fallbackTone shouldBe MessageTone.INFORMATIVE
        NotificationType.HOLIDAY_EXPLORE.fallbackTone shouldBe MessageTone.SUGGESTIVE
    }

    @Test
    fun `변수 토큰`() {
        MessageVariable.RECENT_UPLOAD_DAY.token shouldBe "최근 업로드 요일"
        MessageVariable.HOLIDAY_NAME.token shouldBe "공휴일명"
    }

    @Test
    fun `SendTarget 의 변수는 기본이 빈 맵이다`() {
        val target = SendTarget(userId = 42L, fcmToken = "token-abc")
        target.variables.shouldBeEmpty()
    }

    @Test
    fun `SendTarget 은 받은 변수를 그대로 든다`() {
        val vars = mapOf(MessageVariable.RECENT_UPLOAD_DAY to "지난 토요일")
        SendTarget(userId = 7L, fcmToken = "token-xyz", variables = vars).variables shouldBe vars
    }
}
