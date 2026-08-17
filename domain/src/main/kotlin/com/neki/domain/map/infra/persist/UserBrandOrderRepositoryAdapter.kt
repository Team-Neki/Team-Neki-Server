package com.neki.domain.map.infra.persist

import com.neki.domain.map.infra.persist.jpa.JpaUserBrandOrderRepository
import com.neki.domain.map.models.UserBrandOrder
import com.neki.domain.map.repository.UserBrandOrderRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : UserBrandOrderRepositoryAdapter
 * author         : darren
 * date           : 2026. 6. 22.
 * description    :
 */
@Repository
class UserBrandOrderRepositoryAdapter(private val jpaRepository: JpaUserBrandOrderRepository) :
    UserBrandOrderRepository {

    override fun findSortOrderMapByUserId(userId: Long): Map<Long, Int> =
        jpaRepository.findAllByIdUserId(userId).associate { it.id.brandId to it.sortOrder }

    override fun replaceOrder(userId: Long, orders: List<UserBrandOrder>) {
        jpaRepository.deleteAllByIdUserId(userId)
        // 동일 트랜잭션 내 INSERT 가 DELETE 보다 먼저 실행되어 PK 충돌이 나는 것을 방지
        jpaRepository.flush()

        jpaRepository.saveAll(orders)
    }
}
