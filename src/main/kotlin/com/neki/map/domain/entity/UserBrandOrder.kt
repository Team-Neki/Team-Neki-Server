package com.neki.map.domain.entity

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
    protected constructor() : this(
        UserBrandOrderId(0L, 0L),
        0,
    )

    constructor(userId: Long, brandId: Long, sortOrder: Int) : this(
        UserBrandOrderId(userId, brandId),
        sortOrder,
    )
}

@Embeddable
data class UserBrandOrderId(

    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "brand_id")
    val brandId: Long,

) : Serializable
