package com.neki.domain.map.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * fileName       : BrandTest
 * description    : Brand 도메인 로직 단위 테스트
 */
class BrandTest :
    FunSpec({

        fun brand(): Brand = Brand.of(
            name = "포토그레이",
            code = "PHOTOGRAY",
            mediaId = 1L,
            supportAndroidQr = true,
            supportIosQr = true,
        )

        test("updateInfo - mediaId를 넘기면 이미지가 교체된다") {
            // Given
            val brand = brand()

            // When
            brand.updateInfo(null, null, 2L, null, null, null)

            // Then
            brand.mediaId shouldBe 2L
        }

        test("updateInfo - mediaId가 null이면 기존 이미지를 유지한다") {
            // Given
            val brand = brand()

            // When
            brand.updateInfo("포토이즘", null, null, null, null, null)

            // Then
            brand.mediaId shouldBe 1L
            brand.name shouldBe "포토이즘"
        }
    })
