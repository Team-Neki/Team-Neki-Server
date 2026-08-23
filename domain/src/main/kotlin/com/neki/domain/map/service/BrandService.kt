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

    /**
     * 브랜드 코드로 단건 조회한다. 존재하지 않으면 NOT_FOUND.
     */
    fun getBrandByCode(command: MapCommand.CollectPhotoBooth): Brand =
        brandRepository.getBrand(command.brandCode) ?: throw BusinessException(ResultCode.NOT_FOUND)

    /**
     * brandIds 는 오케스트레이션 중 결정되는 값이므로 query 와 함께 받는다.
     * 조회 결과는 전체 조회와 동일하게 사용자별 정렬 순서를 따른다.
     */
    fun getBrandsByIds(query: MapQuery.GetPolygonBrand, brandIds: List<Long>): List<Brand> {
        val brands: List<Brand> = brandRepository.findAllByIds(brandIds)

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
