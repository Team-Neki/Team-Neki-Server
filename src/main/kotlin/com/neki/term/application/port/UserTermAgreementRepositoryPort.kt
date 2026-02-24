package com.neki.term.application.port

import com.neki.term.domain.entity.UserTermAgreement

interface UserTermAgreementRepositoryPort {
    fun findByUserId(userId: Long): List<UserTermAgreement>

    fun saveAll(agreements: List<UserTermAgreement>): List<UserTermAgreement>
}
