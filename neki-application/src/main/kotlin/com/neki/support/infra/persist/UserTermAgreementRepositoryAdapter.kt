package com.neki.support.infra.persist

import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.entity.UserTermAgreement
import com.neki.support.infra.persist.jpa.JpaUserTermAgreementRepository
import org.springframework.stereotype.Repository

@Repository
class UserTermAgreementRepositoryAdapter(private val jpaRepository: JpaUserTermAgreementRepository) :
    UserTermAgreementRepositoryPort {

    override fun findByUserId(userId: Long): List<UserTermAgreement> = jpaRepository.findAllByIdUserId(userId)

    override fun saveAll(agreements: List<UserTermAgreement>): List<UserTermAgreement> =
        jpaRepository.saveAll(agreements)
}
