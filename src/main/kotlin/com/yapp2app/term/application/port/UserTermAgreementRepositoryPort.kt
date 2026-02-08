package com.yapp2app.term.application.port

import com.yapp2app.term.domain.entity.UserTermAgreement

interface UserTermAgreementRepositoryPort {
    fun findByUserId(userId: Long): List<UserTermAgreement>

    fun saveAll(agreements: List<UserTermAgreement>): List<UserTermAgreement>
}
