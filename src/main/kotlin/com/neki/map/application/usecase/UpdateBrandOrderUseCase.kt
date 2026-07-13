package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.map.application.command.UpdateBrandOrderCommand
import com.neki.map.application.port.BrandRepositoryPort
import com.neki.map.application.port.UserBrandOrderRepositoryPort
import com.neki.map.domain.entity.Brand
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateBrandOrderUseCase
 * author         : darren
 * date           : 2026. 6. 22.
 * description    : 사용자별 브랜드 정렬 순서 저장/갱신
 */
@UseCase
class UpdateBrandOrderUseCase(
    private val brandRepository: BrandRepositoryPort,
    private val userBrandOrderRepository: UserBrandOrderRepositoryPort,
) {

    @Transactional
    fun execute(command: UpdateBrandOrderCommand) {
        val brandIds: List<Long> = command.brandIds

        val existingBrandIds: Set<Long> = brandRepository.findAll().mapNotNull(Brand::id).toSet()
        if (!existingBrandIds.containsAll(brandIds)) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        userBrandOrderRepository.replaceOrder(command.userId, brandIds)
    }
}
