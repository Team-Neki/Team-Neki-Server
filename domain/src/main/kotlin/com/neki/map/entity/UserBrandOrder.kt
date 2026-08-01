package com.neki.map.entity

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

/**
 * fileName       : UserBrandOrder
 * author         : darren
 * date           : 2026. 6. 22.
 * description    : 사용자별 브랜드 정렬 순서 엔티티
 */
@Entity
@Table(name = "TB_USER_BRAND_ORDER")
class UserBrandOrder(

    @EmbeddedId
    val id: UserBrandOrderId,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,
) : BaseTimeEntity() {

    constructor(
        userId: Long,
        brandId: Long,
        sortOrder: Int,
    ) : this(
        id = UserBrandOrderId(
            userId = userId,
            brandId = brandId,
        ),
        sortOrder = sortOrder,
    )

    companion object {
        /**
         * 사용자가 지정한 브랜드 순서를 정렬 엔티티 목록으로 변환한다.
         * 리스트의 위치(index)가 곧 정렬 순서(sortOrder)가 된다.
         */
        fun ofOrderedBrandIds(userId: Long, brandIds: List<Long>): List<UserBrandOrder> =
            brandIds.mapIndexed { index, brandId ->
                UserBrandOrder(userId = userId, brandId = brandId, sortOrder = index)
            }
    }
}

@Embeddable
data class UserBrandOrderId(

    @Column(
        name = "user_id",
        nullable = false,
        updatable = false,
    )
    val userId: Long,

    @Column(
        name = "brand_id",
        nullable = false,
        updatable = false,
    )
    val brandId: Long,

) : Serializable
