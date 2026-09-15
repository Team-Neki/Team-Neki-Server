package com.neki.api.search.application.usecase

import com.neki.api.search.application.SearchPhotoBoothsUseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.UserLocation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSorted
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * fileName       : SearchPhotoBoothsUseCaseTest
 * description    : SearchPhotoBoothsUseCase 단위 테스트 (mock 데이터 기준 정렬·필터·예외)
 */
class SearchPhotoBoothsUseCaseTest :
    FunSpec({

        val userId = 1L
        val gangnamGu = SearchTarget.Region("1168000000")
        val gangnamStation = SearchTarget.Station(name = "강남", lineName = "2호선")
        val gangnamLocation = UserLocation(latitude = 37.4979, longitude = 127.0276)

        val useCase = SearchPhotoBoothsUseCase()

        test("사용자 위치가 있으면 distance 가 채워지고 가까운 순으로 정렬된다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, gangnamGu, brandIds = null, userLocation = gangnamLocation)

            // When
            val result = useCase.execute(query)

            // Then
            result.items shouldHaveSize 4
            result.items.map { it.id } shouldBe listOf(2591L, 3102L, 2604L, 2560L)
            result.items.map { it.distance.shouldNotBeNull() }.shouldBeSorted()
        }

        test("사용자 위치가 없으면 distance 가 null 이고 브랜드 이름, 지점 이름 순으로 정렬된다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, gangnamGu, brandIds = null, userLocation = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.items.map { it.distance } shouldBe listOf(null, null, null, null)
            result.items.map { it.brandName to it.branchName } shouldBe listOf(
                "인생네컷" to "강남역점",
                "포토이즘" to "강남1호점",
                "포토이즘" to "강남역점",
                "포토이즘" to "역삼점",
            )
        }

        test("역을 고르면 역에 딸린 부스 목록을 반환한다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, gangnamStation, brandIds = null, userLocation = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.items shouldHaveSize 6
        }

        test("brandIds 를 주면 그 브랜드의 부스만 반환한다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, gangnamGu, brandIds = listOf(2L), userLocation = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.items shouldHaveSize 1
            result.items[0].brandCode shouldBe "LIFEFOURCUTS"
        }

        test("brandIds 가 빈 목록이면 모든 브랜드를 반환한다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, gangnamGu, brandIds = emptyList(), userLocation = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.items shouldHaveSize 4
        }

        test("즐겨찾기 여부를 그대로 내려준다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, gangnamGu, brandIds = null, userLocation = null)

            // When
            val result = useCase.execute(query)

            // Then
            result.items.filter { it.favorite }.map { it.id } shouldBe listOf(2560L)
        }

        test("부스가 없는 지역은 빈 목록을 반환한다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, SearchTarget.Region("4817010300"), null, null)

            // When
            val result = useCase.execute(query)

            // Then
            result.items.shouldBeEmpty()
        }

        test("없는 지역이면 NOT_FOUND 예외를 던진다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, SearchTarget.Region("9999999999"), null, null)

            // When
            val exception = shouldThrow<BusinessException> { useCase.execute(query) }

            // Then
            exception.resultCode shouldBe ResultCode.NOT_FOUND
        }

        test("없는 역이면 NOT_FOUND 예외를 던진다") {
            // Given
            val query = SearchQuery.GetPhotoBooths(userId, SearchTarget.Station("강남", "9호선"), null, null)

            // When
            val exception = shouldThrow<BusinessException> { useCase.execute(query) }

            // Then
            exception.resultCode shouldBe ResultCode.NOT_FOUND
        }
    })
