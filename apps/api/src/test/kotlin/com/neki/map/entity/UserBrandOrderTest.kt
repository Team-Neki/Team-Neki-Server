package com.neki.map.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * fileName       : UserBrandOrderTest
 * description    : UserBrandOrder 도메인 로직 단위 테스트
 */
class UserBrandOrderTest :
    FunSpec({

        val userId = 1L

        test("ofOrderedBrandIds - 리스트 위치(index)가 정렬 순서(sortOrder)로 부여된다") {
            // Given
            val brandIds = listOf(30L, 10L, 20L)

            // When
            val orders = UserBrandOrder.ofOrderedBrandIds(userId, brandIds)

            // Then
            orders.map { it.id.brandId to it.sortOrder } shouldBe listOf(
                30L to 0,
                10L to 1,
                20L to 2,
            )
            orders.forEach { it.id.userId shouldBe userId }
        }

        test("ofOrderedBrandIds - 빈 목록이면 빈 결과를 반환한다") {
            // When
            val orders = UserBrandOrder.ofOrderedBrandIds(userId, emptyList())

            // Then
            orders.shouldBeEmpty()
        }
    })
