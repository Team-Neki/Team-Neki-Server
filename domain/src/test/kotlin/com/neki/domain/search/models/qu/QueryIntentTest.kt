package com.neki.domain.search.models.qu

import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.qu.QueryIntent
import com.neki.domain.search.models.qu.ResolvedEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * fileName       : QueryIntentTest
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : QueryIntent 는 정규화 검색어로만 만들고, 엔티티 순서와 남은 조각을 스스로 정한다
 */
class QueryIntentTest :
    FunSpec({

        test("정규화하지 않은 검색어로는 만들 수 없다") {
            shouldThrow<IllegalArgumentException> { QueryIntent.of("강남 포토이즘", emptyList()) }
        }

        test("엔티티가 범위 순이 아니어도 범위 순으로 놓고 그 사이 조각을 나눈다") {
            val brand = ResolvedEntity("포토이즘", 8, 12, SearchTarget.Brand(1))
            val region = ResolvedEntity("강남구", 0, 3, SearchTarget.Region("1168000000"))

            val intent: QueryIntent = QueryIntent.of("강남구라이브러리포토이즘", listOf(brand, region))

            intent.entities.map { it.keyword } shouldBe listOf("강남구", "포토이즘")
            intent.remainingTerms shouldBe listOf("라이브러리")
        }
    })
