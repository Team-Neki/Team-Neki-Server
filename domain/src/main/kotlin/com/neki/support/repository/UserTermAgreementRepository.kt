package com.neki.support.repository

import com.neki.support.models.UserTermAgreement

interface UserTermAgreementRepository {
    fun findByUserId(userId: Long): List<UserTermAgreement>

    fun findByUserIdAndTermId(userId: Long, termId: Long): UserTermAgreement?

    fun saveAll(agreements: List<UserTermAgreement>): List<UserTermAgreement>

    fun save(agreement: UserTermAgreement): UserTermAgreement

    fun deleteAllByUserIdAndTermIds(userId: Long, termIds: List<Long>)
}
