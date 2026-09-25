package com.neki.api.search.application.usecase

import com.neki.api.search.application.SearchPhotoBoothsUseCase
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.UserLocation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * fileName       : SearchPhotoBoothsUseCaseTest
 * description    : SearchPhotoBoothsUseCase 단위 테스트 (입력과 무관한 고정 mock 응답)
 */
class SearchPhotoBoothsUseCaseTest :
    FunSpec({

        val userId = 1L
        val useCase = SearchPhotoBoothsUseCase()

        val fixedIds = listOf(2591L, 3115L, 3102L, 2573L, 2604L, 2560L)

        test("keyword, brandIds, userLocation 과 무관하게 항상 같은 부스 6개를 가까운 순으로 반환한다") {
            // Given
            val queries = listOf(
                SearchQuery.GetPhotoBooths(userId, "강남", brandIds = null, userLocation = null),
                SearchQuery.GetPhotoBooths(
                    userId,
                    "송정",
                    brandIds = listOf(2L),
                    userLocation = UserLocation(37.4979, 127.0276),
                ),
                SearchQuery.GetPhotoBooths(userId, "", brandIds = emptyList(), userLocation = UserLocation(0.0, 0.0)),
            )

            // When
            val results = queries.map { useCase.execute(it) }

            // Then
            results.forEach { it.items.map { item -> item.id } shouldBe fixedIds }
            results.distinct() shouldHaveSize 1
        }

        test("distance 는 강남역 기준 고정값이고 오름차순이다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, "강남", brandIds = null, userLocation = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.items.map { it.distance } shouldBe listOf(175, 250, 288, 360, 416, 469)
        }

        test("즐겨찾기 여부를 그대로 내려준다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, "강남", brandIds = null, userLocation = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.items.filter { it.favorite }.map { it.id } shouldBe listOf(2560L)
        }
    })
