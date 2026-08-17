package com.neki.api.map

import com.neki.api.testfixture.aBrand
import com.neki.domain.map.BrandOrderPolicy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * fileName       : BrandOrderPolicyTest
 * description    : BrandOrderPolicy 정렬 규칙 단위 테스트
 */
class BrandOrderPolicyTest :
    FunSpec({

        val brand1 = aBrand(id = 1L, name = "인생네컷", code = "lifefour")
        val brand2 = aBrand(id = 2L, name = "하루필름", code = "harufilm")
        val brand3 = aBrand(id = 3L, name = "포토이즘", code = "photoism")

        test("저장된 순서가 없으면 id 오름차순을 유지한다") {
            // Given
            val brands = listOf(brand3, brand1, brand2)

            // When
            val sorted = BrandOrderPolicy.sort(brands, emptyMap())

            // Then
            sorted.map { it.id } shouldBe listOf(1L, 2L, 3L)
        }

        test("사용자가 커스텀한 순서(sortOrder)대로 정렬한다") {
            // Given
            val brands = listOf(brand1, brand2, brand3)
            val sortOrderMap = mapOf(3L to 0, 1L to 1, 2L to 2)

            // When
            val sorted = BrandOrderPolicy.sort(brands, sortOrderMap)

            // Then
            sorted.map { it.id } shouldBe listOf(3L, 1L, 2L)
        }

        test("커스텀 순서가 있는 브랜드가 앞에, 없는 브랜드는 뒤에 id 순으로 정렬된다") {
            // Given
            val brand4 = aBrand(id = 4L, name = "브랜드4", code = "brand4")
            val brands = listOf(brand1, brand2, brand3, brand4)
            // 사용자는 brand3, brand2 순서만 커스텀 저장. brand1, brand4는 이후 추가된 브랜드로 간주.
            val sortOrderMap = mapOf(3L to 0, 2L to 1)

            // When
            val sorted = BrandOrderPolicy.sort(brands, sortOrderMap)

            // Then
            sorted.map { it.id } shouldBe listOf(3L, 2L, 1L, 4L)
        }

        test("빈 브랜드 목록은 빈 목록을 반환한다") {
            // When
            val sorted = BrandOrderPolicy.sort(emptyList(), mapOf(1L to 0))

            // Then
            sorted shouldBe emptyList()
        }
    })
