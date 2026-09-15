package com.neki.api.search.application.usecase

import com.neki.api.search.application.GetSearchFilterUseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.SearchTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * fileName       : GetSearchFilterUseCaseTest
 * description    : GetSearchFilterUseCase 단위 테스트 (mock 데이터 기준 집계·정렬·예외)
 */
class GetSearchFilterUseCaseTest :
    FunSpec({

        val userId = 1L
        val gangnamGu = SearchTarget.Region("1168000000")
        val gangnamStation = SearchTarget.Station(name = "강남", lineName = "신분당선")

        val useCase = GetSearchFilterUseCase()

        test("목록에 있는 브랜드만 부스 개수와 함께 브랜드 ID 순으로 반환한다") {
            // Given
            val query = SearchQuery.GetFilter(userId, gangnamGu, brandIds = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.brandFilter shouldHaveSize 2
            result.brandFilter.map { Triple(it.id, it.code, it.count) } shouldBe listOf(
                Triple(1L, "PHOTOISM", 3),
                Triple(2L, "LIFEFOURCUTS", 1),
            )
        }

        test("역을 고르면 역에 딸린 부스로 집계한다") {
            // Given
            val query = SearchQuery.GetFilter(userId, gangnamStation, brandIds = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.brandFilter.map { it.id to it.count } shouldBe listOf(1L to 4, 2L to 2)
        }

        test("brandIds 를 주면 그 브랜드만 집계한다") {
            // Given
            val query = SearchQuery.GetFilter(userId, gangnamGu, brandIds = listOf(2L))

            // When
            val result = useCase.execute(query)

            // Then
            result.brandFilter.map { it.id to it.count } shouldBe listOf(2L to 1)
        }

        test("부스가 없는 역은 빈 목록을 반환한다") {
            // Given
            val query = SearchQuery.GetFilter(userId, SearchTarget.Station("강남구청", "7호선"), brandIds = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.brandFilter.shouldBeEmpty()
        }

        test("없는 지역이면 NOT_FOUND 예외를 던진다") {
            // Given
            val query = SearchQuery.GetFilter(userId, SearchTarget.Region("0000000000"), brandIds = null)

            // When
            val exception = shouldThrow<BusinessException> { useCase.execute(query) }

            // Then
            exception.resultCode shouldBe ResultCode.NOT_FOUND
        }
    })
