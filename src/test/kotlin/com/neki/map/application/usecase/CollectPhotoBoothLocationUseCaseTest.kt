package com.neki.map.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.map.application.command.CollectPhotoBoothCommand
import com.neki.map.application.contract.LocalSearchResult
import com.neki.map.application.port.BrandRepositoryPort
import com.neki.map.application.port.MapSearchPort
import com.neki.map.application.port.PhotoBoothLocationRepositoryPort
import com.neki.map.domain.entity.PhotoBoothLocation
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aBrand
import com.neki.testfixture.aPhotoBoothLocation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

/**
 * fileName       : CollectPhotoBoothLocationUseCaseTest
 * description    : CollectPhotoBoothLocationUseCase 단위 테스트
 */
class CollectPhotoBoothLocationUseCaseTest : FunSpec({

    lateinit var brandRepository: BrandRepositoryPort
    lateinit var photoBoothLocationRepository: PhotoBoothLocationRepositoryPort
    lateinit var mapSearch: MapSearchPort
    lateinit var useCase: CollectPhotoBoothLocationUseCase

    beforeTest {
        brandRepository = mockk()
        photoBoothLocationRepository = mockk()
        mapSearch = mockk()
        useCase = CollectPhotoBoothLocationUseCase(
            brandRepository = brandRepository,
            photoBoothLocationRepository = photoBoothLocationRepository,
            transactionRunner = FakeTransactionRunner(),
            mapSearch = mapSearch,
        )
    }

    // GeoPoint.of(latitude, longitude) 순서로 호출되므로 latitude는 -180..180, longitude은 -90..90 범위여야 함
    // 실제 사용 시: GeoPoint.of(place.latitude.toDouble(), place.longitude.toDouble())
    fun aPlace(
        id: String,
        placeName: String = "인생네컷 $id",
        roadAddressName: String = "서울특별시 강남구 테헤란로 $id",
        latitude: String = "37.4979",   // longitude 파라미터로 전달됨 (-180..180 범위)
        longitude: String = "37.0",     // latitude 파라미터로 전달됨 (-90..90 범위)
    ) = LocalSearchResult.Place(
        id = id,
        placeName = placeName,
        roadAddressName = roadAddressName,
        addressName = roadAddressName,
        latitude = latitude,
        longitude = longitude,
        phone = null,
        categoryName = null,
    )

    test("정상 수집 - 신규, 수정, 삭제 location이 올바르게 처리된다") {
        // Given
        val brand = aBrand(id = 1L, code = "lifefour")
        val command = CollectPhotoBoothCommand(keyword = "인생네컷", brandCode = "lifefour")

        // 기존 location: place-1(유지+수정), place-2(삭제 대상)
        val existingLocation1 = aPhotoBoothLocation(id = 10L, mapId = "place-1", brandId = 1L)
        val existingLocation2 = aPhotoBoothLocation(id = 20L, mapId = "place-2", brandId = 1L)

        // API 결과: place-1(수정), place-3(신규)
        val places = listOf(
            aPlace(id = "place-1", placeName = "인생네컷 강남점 수정"),
            aPlace(id = "place-3", placeName = "인생네컷 신규점"),
        )

        every { brandRepository.getBrand("lifefour") } returns brand
        every { photoBoothLocationRepository.getPhotoBoothLocations(1L) } returns listOf(existingLocation1, existingLocation2)
        every { mapSearch.searchAllKorea("인생네컷") } returns places
        every { photoBoothLocationRepository.saveAll(any()) } answers { firstArg() }
        every { photoBoothLocationRepository.deleteAll(any()) } returns Unit

        // When
        val result = useCase.execute(command)

        // Then
        result.collectedCount shouldBe 1   // place-3 신규
        result.duplicatedCount shouldBe 1  // place-1 수정
        result.totalProcessed shouldBe 2

        verify(exactly = 1) { photoBoothLocationRepository.saveAll(any()) }
        verify(exactly = 1) { photoBoothLocationRepository.deleteAll(any()) } // place-2 삭제
    }

    test("브랜드 미존재 - BusinessException이 발생한다") {
        // Given
        val command = CollectPhotoBoothCommand(keyword = "없는브랜드", brandCode = "unknown")

        every { brandRepository.getBrand("unknown") } returns null

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(command)
        }

        exception.resultCode shouldBe ResultCode.NOT_FOUND

        verify(exactly = 0) { mapSearch.searchAllKorea(any()) }
        verify(exactly = 0) { photoBoothLocationRepository.saveAll(any()) }
        verify(exactly = 0) { photoBoothLocationRepository.deleteAll(any()) }
    }

    test("API 결과 없음 - 저장이 호출되지 않는다") {
        // Given
        val brand = aBrand(id = 1L, code = "lifefour")
        val command = CollectPhotoBoothCommand(keyword = "인생네컷", brandCode = "lifefour")

        val existingLocation = aPhotoBoothLocation(id = 10L, mapId = "place-1", brandId = 1L)

        every { brandRepository.getBrand("lifefour") } returns brand
        every { photoBoothLocationRepository.getPhotoBoothLocations(1L) } returns listOf(existingLocation)
        every { mapSearch.searchAllKorea("인생네컷") } returns emptyList()
        every { photoBoothLocationRepository.deleteAll(any()) } returns Unit

        // When
        val result = useCase.execute(command)

        // Then
        result.collectedCount shouldBe 0
        result.duplicatedCount shouldBe 0
        result.totalProcessed shouldBe 0

        verify(exactly = 0) { photoBoothLocationRepository.saveAll(any()) }
    }

    test("기존 location 모두 유지 - API 결과가 기존과 동일하면 삭제가 0건이다") {
        // Given
        val brand = aBrand(id = 1L, code = "lifefour")
        val command = CollectPhotoBoothCommand(keyword = "인생네컷", brandCode = "lifefour")

        val existingLocation1 = aPhotoBoothLocation(id = 10L, mapId = "place-1", brandId = 1L)
        val existingLocation2 = aPhotoBoothLocation(id = 20L, mapId = "place-2", brandId = 1L)

        val places = listOf(
            aPlace(id = "place-1"),
            aPlace(id = "place-2"),
        )

        every { brandRepository.getBrand("lifefour") } returns brand
        every { photoBoothLocationRepository.getPhotoBoothLocations(1L) } returns listOf(existingLocation1, existingLocation2)
        every { mapSearch.searchAllKorea("인생네컷") } returns places
        every { photoBoothLocationRepository.saveAll(any()) } answers { firstArg() }

        // When
        val result = useCase.execute(command)

        // Then
        result.collectedCount shouldBe 0    // 신규 없음
        result.duplicatedCount shouldBe 2   // 모두 수정(유지)
        result.totalProcessed shouldBe 2

        verify(exactly = 0) { photoBoothLocationRepository.deleteAll(any()) }
    }

    test("API 결과 비어있음 + 기존 location 있음 - 기존 location이 전부 삭제된다") {
        // Given
        val brand = aBrand(id = 1L, code = "lifefour")
        val command = CollectPhotoBoothCommand(keyword = "인생네컷", brandCode = "lifefour")

        val existingLocations = listOf(
            aPhotoBoothLocation(id = 10L, mapId = "place-1", brandId = 1L),
            aPhotoBoothLocation(id = 20L, mapId = "place-2", brandId = 1L),
            aPhotoBoothLocation(id = 30L, mapId = "place-3", brandId = 1L),
        )

        every { brandRepository.getBrand("lifefour") } returns brand
        every { photoBoothLocationRepository.getPhotoBoothLocations(1L) } returns existingLocations
        every { mapSearch.searchAllKorea("인생네컷") } returns emptyList()

        val deletedSlot = slot<Collection<PhotoBoothLocation>>()
        every { photoBoothLocationRepository.deleteAll(capture(deletedSlot)) } returns Unit

        // When
        val result = useCase.execute(command)

        // Then
        result.collectedCount shouldBe 0
        result.duplicatedCount shouldBe 0
        result.totalProcessed shouldBe 0

        verify(exactly = 0) { photoBoothLocationRepository.saveAll(any()) }
        verify(exactly = 1) { photoBoothLocationRepository.deleteAll(any()) }
        deletedSlot.captured.size shouldBe 3
    }

    test("mapSearch 예외 - 외부 API 장애 시 예외가 전파되고 기존 데이터에 변경이 없다") {
        // Given
        val brand = aBrand(id = 1L, code = "lifefour")
        val command = CollectPhotoBoothCommand(keyword = "인생네컷", brandCode = "lifefour")

        val existingLocation = aPhotoBoothLocation(id = 10L, mapId = "place-1", brandId = 1L)

        every { brandRepository.getBrand("lifefour") } returns brand
        every { photoBoothLocationRepository.getPhotoBoothLocations(1L) } returns listOf(existingLocation)
        every { mapSearch.searchAllKorea("인생네컷") } throws RuntimeException("외부 API 장애")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(command)
        }

        // 기존 데이터에 저장/삭제 호출 없음
        verify(exactly = 0) { photoBoothLocationRepository.saveAll(any()) }
        verify(exactly = 0) { photoBoothLocationRepository.deleteAll(any()) }
    }
})
