package com.neki.support.application.port

import com.neki.support.entity.UserTermAgreement

interface UserTermAgreementRepositoryPort {
    fun findByUserId(userId: Long): List<UserTermAgreement>

    fun saveAll(agreements: List<UserTermAgreement>): List<UserTermAgreement>
}
