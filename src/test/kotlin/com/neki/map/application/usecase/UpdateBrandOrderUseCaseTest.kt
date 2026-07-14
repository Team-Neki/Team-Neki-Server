package com.neki.map.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.map.application.command.UpdateBrandOrderCommand
import com.neki.map.application.port.BrandRepositoryPort
import com.neki.map.application.port.UserBrandOrderRepositoryPort
import com.neki.map.domain.entity.UserBrandOrder
import com.neki.testfixture.aBrand
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

/**
 * fileName       : UpdateBrandOrderUseCaseTest
 * description    : UpdateBrandOrderUseCase 단위 테스트
 */
class UpdateBrandOrderUseCaseTest :
    FunSpec({

        val userId = 1L

        lateinit var brandRepository: BrandRepositoryPort
        lateinit var userBrandOrderRepository: UserBrandOrderRepositoryPort
        lateinit var useCase: UpdateBrandOrderUseCase

        beforeTest {
            brandRepository = mockk()
            userBrandOrderRepository = mockk()
            useCase = UpdateBrandOrderUseCase(brandRepository, userBrandOrderRepository)
        }

        test("정상 - 모든 brandId가 존재하면 요청 순서대로 sortOrder가 매핑되어 replaceOrder가 한 번 호출된다") {
            // Given
            val brands = listOf(
                aBrand(id = 1L, name = "인생네컷", code = "lifefour"),
                aBrand(id = 2L, name = "하루필름", code = "harufilm"),
                aBrand(id = 3L, name = "포토이즘", code = "photoism"),
            )
            val requestedBrandIds = listOf(3L, 1L, 2L)
            val command = UpdateBrandOrderCommand(userId = userId, brandIds = requestedBrandIds)

            every { brandRepository.findAll() } returns brands

            val ordersSlot = slot<List<UserBrandOrder>>()
            every { userBrandOrderRepository.replaceOrder(userId, capture(ordersSlot)) } returns Unit

            // When
            useCase.execute(command)

            // Then
            val capturedOrders = ordersSlot.captured
            capturedOrders shouldHaveSize 3
            capturedOrders.forEachIndexed { index, order ->
                order.id.userId shouldBe userId
                order.id.brandId shouldBe requestedBrandIds[index]
                order.sortOrder shouldBe index
            }

            verify(exactly = 1) { brandRepository.findAll() }
            verify(exactly = 1) { userBrandOrderRepository.replaceOrder(userId, any()) }
        }

        test("존재하지 않는 brandId 포함 - NOT_FOUND 예외가 발생하고 replaceOrder는 호출되지 않는다") {
            // Given
            val brands = listOf(
                aBrand(id = 1L, name = "인생네컷", code = "lifefour"),
                aBrand(id = 2L, name = "하루필름", code = "harufilm"),
            )
            // 999L 은 findAll 결과에 존재하지 않음
            val command = UpdateBrandOrderCommand(userId = userId, brandIds = listOf(1L, 999L))

            every { brandRepository.findAll() } returns brands

            // When
            val exception = shouldThrow<BusinessException> {
                useCase.execute(command)
            }

            // Then
            exception.resultCode shouldBe ResultCode.NOT_FOUND

            verify(exactly = 1) { brandRepository.findAll() }
            verify(exactly = 0) { userBrandOrderRepository.replaceOrder(any(), any()) }
        }

        test("빈 brandIds - 예외 없이 빈 리스트로 replaceOrder가 한 번 호출된다") {
            // Given
            val brands = listOf(
                aBrand(id = 1L, name = "인생네컷", code = "lifefour"),
            )
            val command = UpdateBrandOrderCommand(userId = userId, brandIds = emptyList())

            every { brandRepository.findAll() } returns brands

            val ordersSlot = slot<List<UserBrandOrder>>()
            every { userBrandOrderRepository.replaceOrder(userId, capture(ordersSlot)) } returns Unit

            // When
            useCase.execute(command)

            // Then
            ordersSlot.captured.shouldBeEmpty()

            verify(exactly = 1) { userBrandOrderRepository.replaceOrder(userId, any()) }
        }
    })
