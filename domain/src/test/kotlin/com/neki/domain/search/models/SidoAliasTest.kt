package com.neki.domain.search.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * fileName       : SidoAliasTest
 * author         : darren
 * date           : 2026. 9. 30.
 * description    : SidoAlias 시도 줄임말 치환 단위 테스트
 */
class SidoAliasTest :
    FunSpec({

        test("expand - 첫 낱말이 줄임말이면 시도 이름으로 바꾼다") {
            // When
            val result: String = SidoAlias.expand("서울 강남")

            // Then
            result shouldBe "서울특별시 강남"
        }

        test("expand - `시` 를 붙인 줄임말과 옛 이름도 바꾼다") {
            SidoAlias.expand("부산시 해운대") shouldBe "부산광역시 해운대"
            SidoAlias.expand("강원도 춘천") shouldBe "강원특별자치도 춘천"
            SidoAlias.expand("전라북도 전주시") shouldBe "전북특별자치도 전주시"
        }

        test("expand - 광주와 전남은 통합된 시도 이름으로 바꾼다") {
            SidoAlias.expand("광주 북구") shouldBe "전남광주통합특별시 북구"
            SidoAlias.expand("전남 목포") shouldBe "전남광주통합특별시 목포"
        }

        test("expand - 공백이 없으면 바꾸지 않는다") {
            SidoAlias.expand("서울") shouldBe "서울"
            SidoAlias.expand("부산진구") shouldBe "부산진구"
        }

        test("expand - 첫 낱말이 줄임말이 아니면 바꾸지 않는다") {
            SidoAlias.expand("서울특별시 강남") shouldBe "서울특별시 강남"
            SidoAlias.expand("수원시 장안") shouldBe "수원시 장안"
            SidoAlias.expand("광주시 오포") shouldBe "광주시 오포"
        }
    })
