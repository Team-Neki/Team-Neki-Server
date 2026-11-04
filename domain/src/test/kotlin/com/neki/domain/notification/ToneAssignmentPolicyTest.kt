package com.neki.domain.notification

import com.neki.domain.notification.models.MessageTone
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** tone = entries[floorMod(userId + businessDate.epochDay, 3)]. 0 INFORMATIVE, 1 FRIENDLY, 2 SUGGESTIVE */
class ToneAssignmentPolicyTest {

    /** epochDay 0 이라 톤이 userId % 3 과 같다 */
    private val epochDay0 = LocalDate.of(1970, 1, 1)

    @Test
    fun `같은 유저라도 발송일이 하루씩 지나면 톤이 순환한다`() {
        ToneAssignmentPolicy.assign(0L, epochDay0) shouldBe MessageTone.INFORMATIVE
        ToneAssignmentPolicy.assign(0L, epochDay0.plusDays(1)) shouldBe MessageTone.FRIENDLY
        ToneAssignmentPolicy.assign(0L, epochDay0.plusDays(2)) shouldBe MessageTone.SUGGESTIVE
        ToneAssignmentPolicy.assign(0L, epochDay0.plusDays(3)) shouldBe MessageTone.INFORMATIVE
    }

    @Test
    fun `같은 날에는 userId 가 1 늘 때마다 톤이 한 칸씩 밀린다`() {
        listOf(
            0L to MessageTone.INFORMATIVE,
            1L to MessageTone.FRIENDLY,
            2L to MessageTone.SUGGESTIVE,
            3L to MessageTone.INFORMATIVE,
            999_999_999_999L to MessageTone.INFORMATIVE,
            999_999_999_998L to MessageTone.SUGGESTIVE,
        ).forEach { (userId, expected) -> ToneAssignmentPolicy.assign(userId, epochDay0) shouldBe expected }
    }

    @Test
    fun `실제 날짜도 epochDay 를 더해 계산한다`() {
        val date = LocalDate.of(2026, 9, 23) // epochDay 20719, floorMod 1
        date.toEpochDay() shouldBe 20719L
        ToneAssignmentPolicy.assign(0L, date) shouldBe MessageTone.FRIENDLY
        ToneAssignmentPolicy.assign(1L, date) shouldBe MessageTone.SUGGESTIVE
        ToneAssignmentPolicy.assign(2L, date) shouldBe MessageTone.INFORMATIVE
    }

    @Test
    fun `userId 가 Long 최대값이어도 덧셈 오버플로 없이 수학적 합의 mod 를 따른다`() {
        // floorMod(MAX, 3) = 1, epochDay 1 -> 2 -> SUGGESTIVE. 그냥 더하면 MIN 으로 감싸져 FRIENDLY 가 된다
        ToneAssignmentPolicy.assign(Long.MAX_VALUE, epochDay0.plusDays(1)) shouldBe MessageTone.SUGGESTIVE
    }

    @Test
    fun `음수 userId 도 floorMod 로 안전하다`() {
        listOf(
            -1L to MessageTone.SUGGESTIVE,
            -2L to MessageTone.FRIENDLY,
            -3L to MessageTone.INFORMATIVE,
            -4L to MessageTone.SUGGESTIVE,
        )
            .forEach { (userId, expected) -> ToneAssignmentPolicy.assign(userId, epochDay0) shouldBe expected }
    }
}
