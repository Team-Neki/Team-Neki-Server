package com.neki.map

import com.neki.map.models.UserBrandOrder

/**
 * fileName       : UserBrandOrderRepositoryPort
 * author         : darren
 * date           : 2026. 6. 22.
 * description    :
 */
interface UserBrandOrderRepository {

    fun findSortOrderMapByUserId(userId: Long): Map<Long, Int>

    fun replaceOrder(userId: Long, orders: List<UserBrandOrder>)
}
