package com.neki.api.map.entity

import com.neki.domain.map.models.PhotoBoothKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * fileName       : PhotoBoothKeyTest
 * description    : 포토부스 키와 부스 id 문자열 변환
 */
class PhotoBoothKeyTest :
    FunSpec({

        test("부스 id 는 지점 마스터 map_id 와 같은 source:<platform>:<idx> 모양이다") {
            PhotoBoothKey(platform = "BROOM_STUDIO", idx = "1768232303").toBoothId() shouldBe
                "source:BROOM_STUDIO:1768232303"
        }

        test("부스 id 를 키로 되돌린다") {
            PhotoBoothKey.fromBoothId("source:BROOM_STUDIO:1768232303") shouldBe
                PhotoBoothKey(platform = "BROOM_STUDIO", idx = "1768232303")
        }

        test("관리자 등록 지점 idx(manual-<id>) 도 되돌린다") {
            PhotoBoothKey.fromBoothId("source:PHOTOISM:manual-3") shouldBe
                PhotoBoothKey(platform = "PHOTOISM", idx = "manual-3")
        }

        test("첫 : 뒤는 전부 idx 로 본다") {
            PhotoBoothKey.fromBoothId("source:PHOTOISM:a:b") shouldBe PhotoBoothKey(platform = "PHOTOISM", idx = "a:b")
        }

        test("모양이 맞지 않으면 null") {
            listOf(
                "1768232303",
                "PHOTOISM:1768232303",
                "source:PHOTOISM",
                "source::1768232303",
                "source:PHOTOISM:",
                "source:${"P".repeat(33)}:1",
                "source:PHOTOISM:${"1".repeat(65)}",
            ).forEach { PhotoBoothKey.fromBoothId(it).shouldBeNull() }
        }
    })
