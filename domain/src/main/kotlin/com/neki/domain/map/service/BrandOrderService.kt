package com.neki.domain.map.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.BrandOrderPolicy
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.UserBrandOrder
import com.neki.domain.map.repository.BrandRepository
import com.neki.domain.map.repository.UserBrandOrderRepository
import org.springframework.stereotype.Component

/**
 * fileName       : BrandOrderService
 * author         : koo
 * date           : 2026. 8. 10.
 * description    : 사용자별 브랜드 노출 순서
 */
@Component
class BrandOrderService(
    private val brandRepository: BrandRepository,
    private val userBrandOrderRepository: UserBrandOrderRepository,
) {

    fun getOrderedBrand(query: MapQuery.GetBrand): List<Brand> {
        val brands: List<Brand> = brandRepository.findAll()

        val sortOrderMap: Map<Long, Int> = userBrandOrderRepository.findSortOrderMapByUserId(query.userId)

        return BrandOrderPolicy.sort(brands, sortOrderMap)
    }

    fun updateBrandOrder(command: MapCommand.UpdateBrandOrder) {
        val brandIds: List<Long> = command.brandIds

        val existingBrandIds: Set<Long> = brandRepository.findAll().mapNotNull(Brand::id).toSet()
        if (!existingBrandIds.containsAll(brandIds)) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        val orders: List<UserBrandOrder> = UserBrandOrder.ofOrderedBrandIds(command.userId, brandIds)
        userBrandOrderRepository.replaceOrder(command.userId, orders)
    }
}
