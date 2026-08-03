package com.neki.map.service

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.map.BrandOrderPolicy
import com.neki.map.BrandRepository
import com.neki.map.UserBrandOrderRepository
import com.neki.map.dto.MapCommand
import com.neki.map.dto.MapQuery
import com.neki.map.models.Brand
import com.neki.map.models.UserBrandOrder
import org.springframework.stereotype.Component

/**
 * fileName       : BrandService
 * author         : koo
 * date           : 2026. 8. 3. 오전 12:26
 * description    :
 */
@Component
class BrandService(
    private val brandRepository: BrandRepository,
    private val userBrandOrderRepository: UserBrandOrderRepository,
) {

    fun getBrand(query: MapQuery.GetBrand): List<Brand> {
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
