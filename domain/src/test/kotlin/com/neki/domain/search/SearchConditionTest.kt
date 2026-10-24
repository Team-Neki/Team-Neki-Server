package com.neki.domain.search

import com.neki.domain.search.models.SearchCondition
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.qu.QueryIntent
import com.neki.domain.search.models.qu.ResolvedEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * fileName       : SearchConditionTest
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 이해한 대상 -> 조회 조건 (지역·역·지점은 범위, 브랜드는 요청 필터와 교집합)
 */
class SearchConditionTest :
    FunSpec({

        val gangnamGu = SearchTarget.Region("1168000000")
        val gangnamLine2 = SearchTarget.Station("강남", "2호선")

        // "강남구포토이즘" 처럼 대상마다 겹치지 않는 범위를 준다
        fun intent(vararg targets: SearchTarget): QueryIntent {
            val keyword: String = "x".repeat(targets.size * 2)
            return QueryIntent.of(keyword, targets.mapIndexed { i, it -> ResolvedEntity("xx", i * 2, i * 2 + 2, it) })
        }

        test("범위(지역·역·지점)가 없으면 조건을 만들 수 없다") {
            SearchCondition.of(intent(SearchTarget.Brand(1)), null).shouldBeNull()
        }

        test("지역·역은 범위가 되고, 브랜드가 없으면 모든 브랜드") {
            val condition: SearchCondition = SearchCondition.of(
                intent(gangnamGu, gangnamLine2),
                emptyList(),
            ).shouldNotBeNull()

            condition.scopes shouldBe listOf(gangnamGu, gangnamLine2)
            condition.brandIds.shouldBeNull()
            condition.matchesNothing shouldBe false
        }

        test("부스 자동완성으로 찾은 지점은 혼자서 범위가 되고, 요청 브랜드 필터를 그대로 받는다") {
            val booth = SearchTarget.Booth(platform = "MONOMANSION", idx = "m1")

            val condition: SearchCondition = SearchCondition.of(intent(booth), listOf(3L)).shouldNotBeNull()

            condition.scopes shouldBe listOf(booth)
            condition.brandIds shouldBe listOf(3L)
        }

        test("검색어의 브랜드나 요청 필터 중 하나만 있으면 그것을 쓴다") {
            SearchCondition.of(intent(gangnamGu, SearchTarget.Brand(1)), null)!!.brandIds shouldBe listOf(1L)
            SearchCondition.of(intent(gangnamGu), listOf(2L))!!.brandIds shouldBe listOf(2L)
        }

        test("둘 다 있으면 교집합이고, 겹치지 않으면 어떤 부스도 맞을 수 없다") {
            val brands = intent(gangnamGu, SearchTarget.Brand(1), SearchTarget.Brand(2))

            SearchCondition.of(brands, listOf(2L, 3L))!!.brandIds shouldBe listOf(2L)
            SearchCondition.of(brands, listOf(3L))!!.matchesNothing shouldBe true
        }
    })
