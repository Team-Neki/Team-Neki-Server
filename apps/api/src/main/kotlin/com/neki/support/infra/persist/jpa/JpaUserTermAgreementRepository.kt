package com.neki.support.infra.persist.jpa

import com.neki.support.models.UserTermAgreement
import com.neki.support.models.UserTermAgreementId
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserTermAgreementRepository : JpaRepository<UserTermAgreement, UserTermAgreementId> {

    fun findAllByIdUserId(userId: Long): List<UserTermAgreement>

    fun findByIdUserIdAndIdTermId(userId: Long, termId: Long): UserTermAgreement?

    fun deleteAllByIdUserIdAndIdTermIdIn(userId: Long, termIds: List<Long>)
}
