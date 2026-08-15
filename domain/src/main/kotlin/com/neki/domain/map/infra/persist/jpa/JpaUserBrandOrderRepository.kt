package com.neki.domain.map.infra.persist.jpa

import com.neki.domain.map.models.UserBrandOrder
import com.neki.domain.map.models.UserBrandOrderId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaUserBrandOrderRepository
 * author         : darren
 * date           : 2026. 6. 22.
 * description    : 사용자별 브랜드 정렬 순서 JPA Repository
 */
interface JpaUserBrandOrderRepository : JpaRepository<UserBrandOrder, UserBrandOrderId> {

    fun findAllByIdUserId(userId: Long): List<UserBrandOrder>

    fun deleteAllByIdUserId(userId: Long)
}
