package com.yapp2app.term.infra.persist

import com.yapp2app.term.application.port.UserTermAgreementRepositoryPort
import com.yapp2app.term.domain.entity.UserTermAgreement
import com.yapp2app.term.infra.persist.jpa.JpaUserTermAgreementRepository
import org.springframework.stereotype.Repository

@Repository
class UserTermAgreementRepositoryAdapter(private val jpaRepository: JpaUserTermAgreementRepository) :
    UserTermAgreementRepositoryPort {

    override fun findByUserId(userId: Long): List<UserTermAgreement> = jpaRepository.findAllByIdUserId(userId)

    override fun saveAll(agreements: List<UserTermAgreement>): List<UserTermAgreement> =
        jpaRepository.saveAll(agreements)
}
