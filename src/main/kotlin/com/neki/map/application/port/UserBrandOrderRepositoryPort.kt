package com.neki.map.application.port

import com.neki.map.domain.entity.UserBrandOrder

/**
 * fileName       : UserBrandOrderRepositoryPort
 * author         : darren
 * date           : 2026. 6. 22.
 * description    :
 */
interface UserBrandOrderRepositoryPort {

    fun findSortOrderMapByUserId(userId: Long): Map<Long, Int>

    fun replaceOrder(userId: Long, orders: List<UserBrandOrder>)
}
