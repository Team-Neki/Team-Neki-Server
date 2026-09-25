package com.neki.api.search.application.usecase

import com.neki.api.search.application.GetSearchFilterUseCase
import com.neki.domain.search.dto.SearchQuery
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * fileName       : GetSearchFilterUseCaseTest
 * description    : GetSearchFilterUseCase 단위 테스트 (입력과 무관한 고정 mock 응답)
 */
class GetSearchFilterUseCaseTest :
    FunSpec({

        val userId = 1L
        val useCase = GetSearchFilterUseCase()

        test("keyword, brandIds 와 무관하게 항상 포토이즘 4, 인생네컷 2 를 브랜드 ID 순으로 반환한다") {
            // Given
            val queries = listOf(
                SearchQuery.GetFilter(userId, "강남", brandIds = null),
                SearchQuery.GetFilter(userId, "송정", brandIds = listOf(2L)),
                SearchQuery.GetFilter(userId, "", brandIds = emptyList()),
            )

            // When
            val results = queries.map { useCase.execute(it) }

            // Then
            results.forEach {
                it.brandFilter.map { b -> Triple(b.id, b.code, b.count) } shouldBe listOf(
                    Triple(1L, "PHOTOISM", 4),
                    Triple(2L, "LIFEFOURCUTS", 2),
                )
            }
            results.distinct() shouldHaveSize 1
        }
    })
