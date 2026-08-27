package com.neki.api.map.application.usecase

import com.neki.api.map.application.GetFilterUseCase
import com.neki.api.testfixture.FakeTransactionRunner
import com.neki.api.testfixture.aBrand
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.repository.BrandRepository
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.PhotoBoothLocationRepository
import com.neki.domain.map.repository.UserBrandOrderRepository
import com.neki.domain.map.service.BrandService
import com.neki.domain.map.service.MapService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

/**
 * fileName       : GetFilterUseCaseTest
 * description    : GetFilterUseCase 단위 테스트
 *
 * 다각형 조회는 PostGIS 함수(ST_Contains 등)에 의존해 H2 기반 E2E 로 검증할 수 없다.
 * 따라서 영역 내 브랜드 필터링과 사용자별 정렬은 이 단위 테스트가 검증한다.
 */
class GetFilterUseCaseTest :
    FunSpec({

        val userId = 1L

        // 강남역 기준 다각형
        val coordinates = listOf(
            Coordinate(127.019128, 37.502456),
            Coordinate(127.035359, 37.502853),
            Coordinate(127.035663, 37.494395),
            Coordinate(127.023675, 37.494257),
            Coordinate(127.019128, 37.502456),
        )

        val geometryFactory = GeometryFactory()

        // 브랜드별 지점 개수를 폴리곤 조회 결과로 만든다. 집계는 UseCase 가 메모리에서 수행한다
        fun booths(vararg countByBrandId: Pair<Long, Int>): List<PhotoBoothLocationView> =
            countByBrandId.flatMap { (brandId: Long, count: Int) ->
                (1..count).map { seq ->
                    PhotoBoothLocationView(
                        id = brandId * 100 + seq,
                        brandId = brandId,
                        brandName = "브랜드$brandId",
                        branchName = "지점$seq",
                        address = "주소",
                        location = geometryFactory.createPoint(Coordinate(127.0, 37.5)),
                    )
                }
            }

        lateinit var photoBoothLocationRepository: PhotoBoothLocationRepository
        lateinit var favoriteMapRepository: FavoriteMapRepository
        lateinit var brandRepository: BrandRepository
        lateinit var userBrandOrderRepository: UserBrandOrderRepository
        lateinit var useCase: GetFilterUseCase

        beforeTest {
            photoBoothLocationRepository = mockk()
            favoriteMapRepository = mockk()
            brandRepository = mockk()
            userBrandOrderRepository = mockk()

            // repository 는 mock, 도메인 서비스는 실제 구현을 사용해 UseCase -> Service -> Repository 경로를 검증한다
            useCase = GetFilterUseCase(
                MapService(favoriteMapRepository, photoBoothLocationRepository),
                BrandService(brandRepository, userBrandOrderRepository),
                FakeTransactionRunner(),
            )

            // 별도 명시가 없으면 저장된 정렬 순서가 없는 사용자로 간주 (서버 기본 순서 유지)
            every { userBrandOrderRepository.findSortOrderMapByUserId(userId) } returns emptyMap()
        }

        test("brandFilter - 영역 내 브랜드만 반환 - 폴리곤 조회 결과에 없는 브랜드는 제외된다") {
            // Given: 영역 내에는 1L, 3L 브랜드만 존재 (2L 은 영역 밖)
            val query = MapQuery.PolygonFilter(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonLocations(coordinates) } returns
                booths(1L to 1, 3L to 2)
            every { brandRepository.findAllByIds(listOf(1L, 3L)) } returns listOf(
                aBrand(id = 1L, name = "인생네컷", code = "lifefour", mediaId = 10L),
                aBrand(id = 3L, name = "포토이즘", code = "photoism", mediaId = 30L),
            )

            // When
            val results = useCase.execute(query).brandFilter

            // Then
            results shouldHaveSize 2
            results.map { it.id } shouldBe listOf(1L, 3L)
            results[0].name shouldBe "인생네컷"
            results[0].code shouldBe "lifefour"
            results[0].count shouldBe 1L
            results[1].name shouldBe "포토이즘"
            results[1].code shouldBe "photoism"
            results[1].count shouldBe 2L

            verify(exactly = 1) { photoBoothLocationRepository.listPolygonLocations(coordinates) }
            verify(exactly = 1) { brandRepository.findAllByIds(listOf(1L, 3L)) }
        }

        test("사용자 정렬 순서 반영 - 저장된 순서대로 반환한다") {
            // Given: 사용자가 3L -> 1L 순으로 정렬을 저장한 상태
            val query = MapQuery.PolygonFilter(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonLocations(coordinates) } returns
                booths(1L to 1, 2L to 2, 3L to 3)
            every { brandRepository.findAllByIds(listOf(1L, 2L, 3L)) } returns listOf(
                aBrand(id = 1L, name = "브랜드1", code = "brand1", mediaId = null),
                aBrand(id = 2L, name = "브랜드2", code = "brand2", mediaId = null),
                aBrand(id = 3L, name = "브랜드3", code = "brand3", mediaId = null),
            )
            every { userBrandOrderRepository.findSortOrderMapByUserId(userId) } returns mapOf(3L to 0, 1L to 1)

            // When
            val results = useCase.execute(query).brandFilter

            // Then: 저장된 순서(3, 1) 이후 미지정 브랜드(2)가 id 순으로 뒤에 붙는다
            results.map { it.id } shouldBe listOf(3L, 1L, 2L)
        }

        test("정렬 순서 미저장 사용자 - 조회 기본 순서(id 오름차순)를 유지한다") {
            // Given
            val query = MapQuery.PolygonFilter(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonLocations(coordinates) } returns
                booths(3L to 1, 1L to 2, 2L to 3)
            every { brandRepository.findAllByIds(listOf(3L, 1L, 2L)) } returns listOf(
                aBrand(id = 3L, name = "브랜드3", code = "brand3", mediaId = null),
                aBrand(id = 1L, name = "브랜드1", code = "brand1", mediaId = null),
                aBrand(id = 2L, name = "브랜드2", code = "brand2", mediaId = null),
            )

            // When
            val results = useCase.execute(query).brandFilter

            // Then
            results.map { it.id } shouldBe listOf(1L, 2L, 3L)
        }

        test("일부 브랜드만 정렬 저장 - 저장된 브랜드가 앞, 나머지는 뒤쪽 id 순이다") {
            // Given: 2L 만 최상단으로 저장한 상태
            val query = MapQuery.PolygonFilter(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonLocations(coordinates) } returns
                booths(1L to 1, 2L to 2, 3L to 3)
            every { brandRepository.findAllByIds(listOf(1L, 2L, 3L)) } returns listOf(
                aBrand(id = 1L, name = "브랜드1", code = "brand1", mediaId = null),
                aBrand(id = 2L, name = "브랜드2", code = "brand2", mediaId = null),
                aBrand(id = 3L, name = "브랜드3", code = "brand3", mediaId = null),
            )
            every { userBrandOrderRepository.findSortOrderMapByUserId(userId) } returns mapOf(2L to 0)

            // When
            val results = useCase.execute(query).brandFilter

            // Then
            results.map { it.id } shouldBe listOf(2L, 1L, 3L)
        }

        test("영역 내 포토부스 없음 - 빈 리스트를 반환하고 브랜드를 조회하지 않는다") {
            // Given
            val query = MapQuery.PolygonFilter(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonLocations(coordinates) } returns emptyList()

            // When
            val results = useCase.execute(query).brandFilter

            // Then
            results.shouldBeEmpty()
            verify(exactly = 0) { brandRepository.findAllByIds(any()) }
            verify(exactly = 0) { userBrandOrderRepository.findSortOrderMapByUserId(any()) }
        }

        test("count - 정렬로 순서가 뒤바뀌어도 개수는 브랜드를 따라간다") {
            // Given: 개수는 1L=10, 2L=20, 3L=30 이고 사용자는 3L -> 1L 순으로 정렬을 저장
            val query = MapQuery.PolygonFilter(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonLocations(coordinates) } returns
                booths(1L to 10, 2L to 20, 3L to 30)
            every { brandRepository.findAllByIds(listOf(1L, 2L, 3L)) } returns listOf(
                aBrand(id = 1L, name = "브랜드1", code = "brand1", mediaId = null),
                aBrand(id = 2L, name = "브랜드2", code = "brand2", mediaId = null),
                aBrand(id = 3L, name = "브랜드3", code = "brand3", mediaId = null),
            )
            every { userBrandOrderRepository.findSortOrderMapByUserId(userId) } returns mapOf(3L to 0, 1L to 1)

            // When
            val results = useCase.execute(query).brandFilter

            // Then: 순서는 3, 1, 2 지만 각 개수는 브랜드 id 에 맞게 붙는다
            results.map { it.id } shouldBe listOf(3L, 1L, 2L)
            results.map { it.count } shouldBe listOf(30L, 10L, 20L)
        }

        test("brandIds 필터 - 쿼리는 영역 전체를 조회하고 필터는 메모리에서 적용된다") {
            // Given: 영역 내에는 1L, 3L 이 있고 요청 필터는 1L, 2L
            val filter = listOf(1L, 2L)
            val query = MapQuery.PolygonFilter(userId = userId, coordinates = coordinates, brandIds = filter)

            every { photoBoothLocationRepository.listPolygonLocations(coordinates) } returns
                booths(1L to 1, 3L to 2)
            every { brandRepository.findAllByIds(listOf(1L)) } returns listOf(
                aBrand(id = 1L, name = "인생네컷", code = "lifefour", mediaId = null),
            )

            // When
            val results = useCase.execute(query).brandFilter

            // Then: 필터와 영역의 교집합만 반환
            results shouldHaveSize 1
            results[0].id shouldBe 1L

            // 쿼리에는 브랜드 조건이 걸리지 않는다 (지도 조회와 동일한 쿼리를 공유)
            verify(exactly = 1) { photoBoothLocationRepository.listPolygonLocations(coordinates) }
        }
    })
