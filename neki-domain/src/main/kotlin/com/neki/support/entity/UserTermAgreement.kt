package com.neki.support.entity

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDateTime

@Entity
@Table(name = "TB_USER_TERM_AGREEMENT")
class UserTermAgreement(
    @EmbeddedId
    val id: UserTermAgreementId,

    @Column(name = "agreed_at", nullable = false)
    val agreedAt: LocalDateTime,

    @Column(name = "term_version", nullable = false, length = 20)
    val termVersion: String,
) : BaseTimeEntity() {

    constructor(
        userId: Long,
        termId: Long,
        agreedAt: LocalDateTime,
        termVersion: String,
    ) : this(
        id = UserTermAgreementId(
            userId = userId,
            termId = termId,
        ),
        agreedAt = agreedAt,
        termVersion = termVersion,
    )
}

@Embeddable
data class UserTermAgreementId(
    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "term_id")
    val termId: Long,
) : Serializable
