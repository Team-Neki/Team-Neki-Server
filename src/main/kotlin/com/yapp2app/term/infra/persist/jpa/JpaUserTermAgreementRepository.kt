package com.yapp2app.term.infra.persist.jpa

import com.yapp2app.term.domain.entity.UserTermAgreement
import com.yapp2app.term.domain.entity.UserTermAgreementId
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserTermAgreementRepository : JpaRepository<UserTermAgreement, UserTermAgreementId> {

    fun findAllByIdUserId(userId: Long): List<UserTermAgreement>
}
