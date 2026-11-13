package com.neki.domain.pose.models

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PoseRelationTest {

    @Test
    @DisplayName("tag 는 대소문자와 앞뒤 공백을 무시한다")
    fun fromTagIgnoresCaseAndSpaces() {
        PoseRelation.fromTag("friend") shouldBe PoseRelation.FRIEND
        PoseRelation.fromTag(" Couple ") shouldBe PoseRelation.COUPLE
        PoseRelation.fromTag("SSUM") shouldBe PoseRelation.SSUM
    }

    @Test
    @DisplayName("빈 값과 null 은 관계 없음(null)")
    fun blankIsNull() {
        PoseRelation.fromTag("") shouldBe null
        PoseRelation.fromTag("   ") shouldBe null
        PoseRelation.fromTag(null) shouldBe null
    }

    @Test
    @DisplayName("모르는 값은 예외")
    fun unknownThrows() {
        shouldThrow<IllegalArgumentException> { PoseRelation.fromTag("lover") }
    }

    @Test
    @DisplayName("관계는 2인 포즈에만 허용한다")
    fun onlyTwo() {
        HeadCount.entries.filter { PoseRelation.isAllowedFor(it) } shouldBe listOf(HeadCount.TWO)
    }
}
