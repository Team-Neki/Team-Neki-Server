package com.neki.map.infra.persist

import com.neki.map.application.port.UserBrandOrderRepositoryPort
import com.neki.map.domain.entity.UserBrandOrder
import com.neki.map.infra.persist.jpa.JpaUserBrandOrderRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : UserBrandOrderRepositoryAdapter
 * author         : darren
 * date           : 2026. 6. 22.
 * description    :
 */
@Repository
class UserBrandOrderRepositoryAdapter(private val jpaRepository: JpaUserBrandOrderRepository) :
    UserBrandOrderRepositoryPort {

    override fun findSortOrderMapByUserId(userId: Long): Map<Long, Int> =
        jpaRepository.findAllByIdUserId(userId).associate { it.id.brandId to it.sortOrder }

    override fun replaceOrder(userId: Long, orders: List<UserBrandOrder>) {
        jpaRepository.deleteAllByIdUserId(userId)
        // 동일 트랜잭션 내 INSERT 가 DELETE 보다 먼저 실행되어 PK 충돌이 나는 것을 방지
        jpaRepository.flush()

        jpaRepository.saveAll(orders)
    }
}
