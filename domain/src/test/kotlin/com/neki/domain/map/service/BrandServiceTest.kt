package com.neki.domain.map.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.BrandCommand
import com.neki.domain.map.models.Brand
import com.neki.domain.map.repository.BrandRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * fileName       : BrandServiceTest
 * description    : BrandService 수정 검증 단위 테스트
 */
class BrandServiceTest :
    FunSpec({

        val brandRepository = mockk<BrandRepository>()
        val brandService = BrandService(brandRepository)

        fun command(
            name: String? = null,
            code: String? = null,
            mediaId: Long? = null,
            supportAndroidQr: Boolean? = null,
            supportIosQr: Boolean? = null,
            exposeToMap: Boolean? = null,
        ): BrandCommand.UpdateBrand =
            BrandCommand.UpdateBrand(1L, name, code, mediaId, supportAndroidQr, supportIosQr, exposeToMap)

        test("updateBrand - 변경할 필드가 하나도 없으면 INVALID_PARAMETER") {
            // When
            val ex = shouldThrow<BusinessException> {
                brandService.updateBrand(command())
            }

            // Then
            ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        }

        test("updateBrand - mediaId만 넘겨도 유효한 변경으로 인정되고 이미지가 교체된다") {
            // Given
            val brand = Brand.of("포토그레이", "PHOTOGRAY", 1L, supportAndroidQr = true, supportIosQr = true)
            every { brandRepository.findById(1L) } returns brand

            // When
            val updated = brandService.updateBrand(command(mediaId = 2L))

            // Then
            updated.mediaId shouldBe 2L
            updated.name shouldBe "포토그레이"
        }
    })
