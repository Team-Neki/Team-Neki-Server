package com.neki.user.domain.entity

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate

/**
 * fileName       : AppleUserTransfer
 * description    : Apple App Transfer 사용자 식별자 매핑 (old_sub -> transfer_sub -> new_sub).
 *                  매핑은 운영자가 사전 적재하며, 로그인 시 new_sub 로 기존 사용자를 식별하는 데 사용된다.
 */
@DynamicUpdate
@Entity
@Table(name = "TB_APPLE_USER_TRANSFER")
class AppleUserTransfer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "old_sub", nullable = false, length = 255)
    val oldSub: String,

    @Column(name = "transfer_sub", nullable = false, length = 255)
    val transferSub: String,

    @Column(name = "new_sub", nullable = true, length = 255)
    val newSub: String? = null,
) : BaseTimeEntity()
