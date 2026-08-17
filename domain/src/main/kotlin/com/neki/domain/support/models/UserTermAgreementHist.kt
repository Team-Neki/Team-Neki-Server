package com.neki.domain.support.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "TB_USER_TERM_AGREEMENT_HIST")
@EntityListeners(AuditingEntityListener::class)
class UserTermAgreementHist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "term_id", nullable = false)
    val termId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    val action: TermAgreementAction,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,
) {
    constructor() : this(
        userId = 0L,
        termId = 0L,
        action = TermAgreementAction.AGREED,
    )

    companion object {
        fun agreed(userId: Long, termId: Long) =
            UserTermAgreementHist(userId = userId, termId = termId, action = TermAgreementAction.AGREED)

        fun withdrawn(userId: Long, termId: Long) =
            UserTermAgreementHist(userId = userId, termId = termId, action = TermAgreementAction.WITHDRAWN)
    }
}
