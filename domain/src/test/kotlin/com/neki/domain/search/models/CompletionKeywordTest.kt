package com.neki.domain.search.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * fileName       : CompletionKeywordTest
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : CompletionKeyword 검색어 낱말 규칙 단위 테스트
 */
class CompletionKeywordTest :
    FunSpec({

        val brandNames = listOf("포토이즘", "포토그레이", "플랜비 스튜디오", "인생네컷")

        test("boothTerms - 낱말마다 묶음을 만들고 시도 이름은 줄임말까지 묶는다") {
            // When
            val result: List<List<String>> = CompletionKeyword.boothTerms("서울 강남 포토그레이")

            // Then
            result shouldBe listOf(listOf("서울특별시", "서울", "서울시"), listOf("강남"), listOf("포토그레이"))
        }

        test("withoutBrandWords - 브랜드 이름 낱말을 뺀다") {
            CompletionKeyword.withoutBrandWords("강남 포토그레이", brandNames) shouldBe "강남"
            CompletionKeyword.withoutBrandWords("포토그레이 강남역 2호선", brandNames) shouldBe "강남역 2호선"
        }

        test("withoutBrandWords - 브랜드 이름의 앞부분이나 여러 낱말 브랜드의 낱말도 뺀다") {
            CompletionKeyword.withoutBrandWords("포토 강남", brandNames) shouldBe "강남"
            CompletionKeyword.withoutBrandWords("플랜비 스튜디오 강남", brandNames) shouldBe "강남"
            CompletionKeyword.withoutBrandWords("플랜비스튜디오 강남", brandNames) shouldBe "강남"
        }

        test("withoutBrandWords - 낱말이 하나면 브랜드 낱말이어도 그대로 둔다") {
            CompletionKeyword.withoutBrandWords("포토그레이", brandNames) shouldBe "포토그레이"
        }

        test("withoutBrandWords - 브랜드 낱말만 있으면 빈 문자열") {
            CompletionKeyword.withoutBrandWords("포토이즘 포토그레이", brandNames) shouldBe ""
        }

        test("withoutBrandWords - 1자 낱말은 브랜드 낱말로 보지 않는다") {
            CompletionKeyword.withoutBrandWords("강남 포", brandNames) shouldBe "강남 포"
        }
    })
