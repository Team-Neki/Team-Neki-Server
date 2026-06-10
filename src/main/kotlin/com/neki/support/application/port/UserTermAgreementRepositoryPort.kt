package com.neki.support.application.port

import com.neki.support.domain.entity.UserTermAgreement

interface UserTermAgreementRepositoryPort {
    fun findByUserId(userId: Long): List<UserTermAgreement>

    fun findByUserIdAndTermId(userId: Long, termId: Long): UserTermAgreement?

    fun saveAll(agreements: List<UserTermAgreement>): List<UserTermAgreement>

    fun save(agreement: UserTermAgreement): UserTermAgreement

    fun deleteAllByUserIdAndTermIds(userId: Long, termIds: List<Long>)
}
