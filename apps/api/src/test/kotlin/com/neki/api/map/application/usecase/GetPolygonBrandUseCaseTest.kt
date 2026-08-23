package com.neki.api.map.application.usecase

import com.neki.api.map.application.GetPolygonBrandUseCase
import com.neki.api.testfixture.FakeTransactionRunner
import com.neki.api.testfixture.aBrand
import com.neki.domain.map.client.MediaClient
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.MediaMetadata
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

/**
 * fileName       : GetPolygonBrandUseCaseTest
 * description    : GetPolygonBrandUseCase 단위 테스트
 *
 * 다각형 조회는 PostGIS 함수(ST_Contains 등)에 의존해 H2 기반 E2E 로 검증할 수 없다.
 * 따라서 영역 내 브랜드 필터링과 사용자별 정렬은 이 단위 테스트가 검증한다.
 */
class GetPolygonBrandUseCaseTest :
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

        lateinit var photoBoothLocationRepository: PhotoBoothLocationRepository
        lateinit var favoriteMapRepository: FavoriteMapRepository
        lateinit var brandRepository: BrandRepository
        lateinit var userBrandOrderRepository: UserBrandOrderRepository
        lateinit var mediaClient: MediaClient
        lateinit var useCase: GetPolygonBrandUseCase

        beforeTest {
            photoBoothLocationRepository = mockk()
            favoriteMapRepository = mockk()
            brandRepository = mockk()
            userBrandOrderRepository = mockk()
            mediaClient = mockk()

            // repository 는 mock, 도메인 서비스는 실제 구현을 사용해 UseCase -> Service -> Repository 경로를 검증한다
            useCase = GetPolygonBrandUseCase(
                MapService(favoriteMapRepository, photoBoothLocationRepository),
                BrandService(brandRepository, userBrandOrderRepository),
                mediaClient,
                FakeTransactionRunner(),
            )

            // 별도 명시가 없으면 저장된 정렬 순서가 없는 사용자로 간주 (서버 기본 순서 유지)
            every { userBrandOrderRepository.findSortOrderMapByUserId(userId) } returns emptyMap()
        }

        test("영역 내 브랜드만 반환 - 폴리곤 조회 결과에 없는 브랜드는 제외된다") {
            // Given: 영역 내에는 1L, 3L 브랜드만 존재 (2L 은 영역 밖)
            val query = MapQuery.GetPolygonBrand(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonBrandIds(coordinates, null) } returns listOf(1L, 3L)
            every { brandRepository.findAllByIds(listOf(1L, 3L)) } returns listOf(
                aBrand(id = 1L, name = "인생네컷", code = "lifefour", mediaId = 10L),
                aBrand(id = 3L, name = "포토이즘", code = "photoism", mediaId = 30L),
            )
            every { mediaClient.getMediaMetadata(listOf(10L, 30L)) } returns listOf(
                MediaMetadata(mediaId = 10L, storageKey = "brand/lifefour.jpg", contentType = "image/jpeg"),
                MediaMetadata(mediaId = 30L, storageKey = "brand/photoism.jpg", contentType = "image/jpeg"),
            )

            // When
            val results = useCase.execute(query)

            // Then
            results shouldHaveSize 2
            results.map { it.id } shouldBe listOf(1L, 3L)
            results[0].name shouldBe "인생네컷"
            results[0].storageKey shouldBe "brand/lifefour.jpg"
            results[1].name shouldBe "포토이즘"
            results[1].storageKey shouldBe "brand/photoism.jpg"

            verify(exactly = 1) { photoBoothLocationRepository.listPolygonBrandIds(coordinates, null) }
            verify(exactly = 1) { brandRepository.findAllByIds(listOf(1L, 3L)) }
        }

        test("사용자 정렬 순서 반영 - 저장된 순서대로 반환한다") {
            // Given: 사용자가 3L -> 1L 순으로 정렬을 저장한 상태
            val query = MapQuery.GetPolygonBrand(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonBrandIds(coordinates, null) } returns listOf(1L, 2L, 3L)
            every { brandRepository.findAllByIds(listOf(1L, 2L, 3L)) } returns listOf(
                aBrand(id = 1L, name = "브랜드1", code = "brand1", mediaId = null),
                aBrand(id = 2L, name = "브랜드2", code = "brand2", mediaId = null),
                aBrand(id = 3L, name = "브랜드3", code = "brand3", mediaId = null),
            )
            every { userBrandOrderRepository.findSortOrderMapByUserId(userId) } returns mapOf(3L to 0, 1L to 1)
            every { mediaClient.getMediaMetadata(emptyList()) } returns emptyList()

            // When
            val results = useCase.execute(query)

            // Then: 저장된 순서(3, 1) 이후 미지정 브랜드(2)가 id 순으로 뒤에 붙는다
            results.map { it.id } shouldBe listOf(3L, 1L, 2L)
        }

        test("정렬 순서 미저장 사용자 - 조회 기본 순서(id 오름차순)를 유지한다") {
            // Given
            val query = MapQuery.GetPolygonBrand(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonBrandIds(coordinates, null) } returns listOf(3L, 1L, 2L)
            every { brandRepository.findAllByIds(listOf(3L, 1L, 2L)) } returns listOf(
                aBrand(id = 3L, name = "브랜드3", code = "brand3", mediaId = null),
                aBrand(id = 1L, name = "브랜드1", code = "brand1", mediaId = null),
                aBrand(id = 2L, name = "브랜드2", code = "brand2", mediaId = null),
            )
            every { mediaClient.getMediaMetadata(emptyList()) } returns emptyList()

            // When
            val results = useCase.execute(query)

            // Then
            results.map { it.id } shouldBe listOf(1L, 2L, 3L)
        }

        test("일부 브랜드만 정렬 저장 - 저장된 브랜드가 앞, 나머지는 뒤쪽 id 순이다") {
            // Given: 2L 만 최상단으로 저장한 상태
            val query = MapQuery.GetPolygonBrand(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonBrandIds(coordinates, null) } returns listOf(1L, 2L, 3L)
            every { brandRepository.findAllByIds(listOf(1L, 2L, 3L)) } returns listOf(
                aBrand(id = 1L, name = "브랜드1", code = "brand1", mediaId = null),
                aBrand(id = 2L, name = "브랜드2", code = "brand2", mediaId = null),
                aBrand(id = 3L, name = "브랜드3", code = "brand3", mediaId = null),
            )
            every { userBrandOrderRepository.findSortOrderMapByUserId(userId) } returns mapOf(2L to 0)
            every { mediaClient.getMediaMetadata(emptyList()) } returns emptyList()

            // When
            val results = useCase.execute(query)

            // Then
            results.map { it.id } shouldBe listOf(2L, 1L, 3L)
        }

        test("영역 내 포토부스 없음 - 빈 리스트를 반환하고 브랜드/미디어를 조회하지 않는다") {
            // Given
            val query = MapQuery.GetPolygonBrand(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonBrandIds(coordinates, null) } returns emptyList()

            // When
            val results = useCase.execute(query)

            // Then
            results.shouldBeEmpty()
            verify(exactly = 0) { brandRepository.findAllByIds(any()) }
            verify(exactly = 0) { mediaClient.getMediaMetadata(any()) }
            verify(exactly = 0) { userBrandOrderRepository.findSortOrderMapByUserId(any()) }
        }

        test("brandIds 필터 - 요청 필터가 폴리곤 조회로 그대로 전달된다") {
            // Given: 1L, 2L 로 필터링 요청. 영역 내에 실제로 존재하는 것은 1L 뿐
            val filter = listOf(1L, 2L)
            val query = MapQuery.GetPolygonBrand(userId = userId, coordinates = coordinates, brandIds = filter)

            every { photoBoothLocationRepository.listPolygonBrandIds(coordinates, filter) } returns listOf(1L)
            every { brandRepository.findAllByIds(listOf(1L)) } returns listOf(
                aBrand(id = 1L, name = "인생네컷", code = "lifefour", mediaId = null),
            )
            every { mediaClient.getMediaMetadata(emptyList()) } returns emptyList()

            // When
            val results = useCase.execute(query)

            // Then: 필터와 영역의 교집합만 반환
            results shouldHaveSize 1
            results[0].id shouldBe 1L

            verify(exactly = 1) { photoBoothLocationRepository.listPolygonBrandIds(coordinates, filter) }
        }

        test("미디어 없는 브랜드 - mediaId 가 null 이면 storageKey 가 null 이다") {
            // Given
            val query = MapQuery.GetPolygonBrand(userId = userId, coordinates = coordinates, brandIds = null)

            every { photoBoothLocationRepository.listPolygonBrandIds(coordinates, null) } returns listOf(1L, 2L)
            every { brandRepository.findAllByIds(listOf(1L, 2L)) } returns listOf(
                aBrand(id = 1L, name = "인생네컷", code = "lifefour", mediaId = 10L),
                aBrand(id = 2L, name = "하루필름", code = "harufilm", mediaId = null),
            )
            every { mediaClient.getMediaMetadata(listOf(10L)) } returns listOf(
                MediaMetadata(mediaId = 10L, storageKey = "brand/lifefour.jpg", contentType = "image/jpeg"),
            )

            // When
            val results = useCase.execute(query)

            // Then
            results shouldHaveSize 2
            results[0].storageKey shouldBe "brand/lifefour.jpg"
            results[1].storageKey shouldBe null
        }
    })
