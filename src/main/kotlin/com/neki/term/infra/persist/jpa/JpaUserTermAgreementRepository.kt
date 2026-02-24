package com.neki.term.infra.persist.jpa

import com.neki.term.domain.entity.UserTermAgreement
import com.neki.term.domain.entity.UserTermAgreementId
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserTermAgreementRepository : JpaRepository<UserTermAgreement, UserTermAgreementId> {

    fun findAllByIdUserId(userId: Long): List<UserTermAgreement>
}
