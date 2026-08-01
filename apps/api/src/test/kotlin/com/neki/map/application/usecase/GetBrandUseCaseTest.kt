package com.neki.map.application.usecase

import com.neki.map.application.port.BrandRepositoryPort
import com.neki.map.application.port.MediaClientPort
import com.neki.map.application.port.UserBrandOrderRepositoryPort
import com.neki.photo.application.port.dto.MediaContract
import com.neki.testfixture.aBrand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * fileName       : GetBrandUseCaseTest
 * description    : GetBrandUseCase 단위 테스트
 */
class GetBrandUseCaseTest :
    FunSpec({

        val userId = 1L

        lateinit var brandRepository: BrandRepositoryPort
        lateinit var mediaClient: MediaClientPort
        lateinit var userBrandOrderRepository: UserBrandOrderRepositoryPort
        lateinit var useCase: GetBrandUseCase

        beforeTest {
            brandRepository = mockk()
            mediaClient = mockk()
            userBrandOrderRepository = mockk()
            useCase = GetBrandUseCase(brandRepository, mediaClient, userBrandOrderRepository)

            // 별도 명시가 없으면 저장된 정렬 순서가 없는 사용자로 간주 (서버 기본 순서 유지)
            every { userBrandOrderRepository.findSortOrderMapByUserId(userId) } returns emptyMap()
        }

        test("정상 조회 - 브랜드 목록과 미디어 storageKey가 올바르게 매핑된다") {
            // Given
            val brand1 = aBrand(id = 1L, name = "인생네컷", code = "lifefour", mediaId = 10L)
            val brand2 = aBrand(id = 2L, name = "하루필름", code = "harufilm", mediaId = 20L)
            val brands = listOf(brand1, brand2)

            val storageInfo1 =
                MediaContract.StorageInfo(mediaId = 10L, storageKey = "brand/lifefour.jpg", contentType = "image/jpeg")
            val storageInfo2 =
                MediaContract.StorageInfo(mediaId = 20L, storageKey = "brand/harufilm.jpg", contentType = "image/jpeg")

            every { brandRepository.findAll() } returns brands
            every { mediaClient.getMediaStorageInfos(listOf(10L, 20L)) } returns listOf(storageInfo1, storageInfo2)

            // When
            val results = useCase.execute(userId)

            // Then
            results shouldHaveSize 2
            results[0].id shouldBe 1L
            results[0].name shouldBe "인생네컷"
            results[0].code shouldBe "lifefour"
            results[0].storageKey shouldBe "brand/lifefour.jpg"
            results[1].id shouldBe 2L
            results[1].name shouldBe "하루필름"
            results[1].code shouldBe "harufilm"
            results[1].storageKey shouldBe "brand/harufilm.jpg"

            verify(exactly = 1) { brandRepository.findAll() }
            verify(exactly = 1) { mediaClient.getMediaStorageInfos(listOf(10L, 20L)) }
        }

        test("미디어 없는 브랜드 - mediaId가 null이면 storageKey가 null로 처리된다") {
            // Given
            val brandWithMedia = aBrand(id = 1L, name = "인생네컷", code = "lifefour", mediaId = 10L)
            val brandWithoutMedia = aBrand(id = 2L, name = "하루필름", code = "harufilm", mediaId = null)

            every { brandRepository.findAll() } returns listOf(brandWithMedia, brandWithoutMedia)
            every { mediaClient.getMediaStorageInfos(listOf(10L)) } returns listOf(
                MediaContract.StorageInfo(mediaId = 10L, storageKey = "brand/lifefour.jpg", contentType = "image/jpeg"),
            )

            // When
            val results = useCase.execute(userId)

            // Then
            results shouldHaveSize 2
            results[0].storageKey shouldBe "brand/lifefour.jpg"
            results[1].storageKey shouldBe null
        }

        test("모든 브랜드 mediaId null - mediaClient에 빈 리스트 전달 후 모든 storageKey가 null이다") {
            // Given
            val brand1 = aBrand(id = 1L, name = "인생네컷", code = "lifefour", mediaId = null)
            val brand2 = aBrand(id = 2L, name = "하루필름", code = "harufilm", mediaId = null)

            every { brandRepository.findAll() } returns listOf(brand1, brand2)
            every { mediaClient.getMediaStorageInfos(emptyList()) } returns emptyList()

            // When
            val results = useCase.execute(userId)

            // Then
            results shouldHaveSize 2
            results[0].storageKey shouldBe null
            results[1].storageKey shouldBe null

            verify(exactly = 1) { mediaClient.getMediaStorageInfos(emptyList()) }
        }

        test("미디어 부분 조회 실패 - 5개 브랜드 중 2개 미디어 미존재 시 해당 브랜드 storageKey가 null이다") {
            // Given
            val brands = (1L..5L).map { id ->
                aBrand(id = id, name = "브랜드$id", code = "brand$id", mediaId = id * 10L)
            }
            val mediaIds = brands.mapNotNull { it.mediaId }

            // 3개만 storageInfo 반환 (40L, 50L은 미존재)
            val availableStorageInfos = listOf(
                MediaContract.StorageInfo(mediaId = 10L, storageKey = "brand/brand1.jpg", contentType = "image/jpeg"),
                MediaContract.StorageInfo(mediaId = 20L, storageKey = "brand/brand2.jpg", contentType = "image/jpeg"),
                MediaContract.StorageInfo(mediaId = 30L, storageKey = "brand/brand3.jpg", contentType = "image/jpeg"),
            )

            every { brandRepository.findAll() } returns brands
            every { mediaClient.getMediaStorageInfos(mediaIds) } returns availableStorageInfos

            // When
            val results = useCase.execute(userId)

            // Then
            results shouldHaveSize 5
            results[0].storageKey shouldBe "brand/brand1.jpg"
            results[1].storageKey shouldBe "brand/brand2.jpg"
            results[2].storageKey shouldBe "brand/brand3.jpg"
            results[3].storageKey shouldBe null // mediaId 40L 미존재
            results[4].storageKey shouldBe null // mediaId 50L 미존재
        }
    })
