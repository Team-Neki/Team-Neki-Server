package com.neki.term.infra.persist

import com.neki.term.application.port.UserTermAgreementRepositoryPort
import com.neki.term.domain.entity.UserTermAgreement
import com.neki.term.infra.persist.jpa.JpaUserTermAgreementRepository
import org.springframework.stereotype.Repository

@Repository
class UserTermAgreementRepositoryAdapter(private val jpaRepository: JpaUserTermAgreementRepository) :
    UserTermAgreementRepositoryPort {

    override fun findByUserId(userId: Long): List<UserTermAgreement> = jpaRepository.findAllByIdUserId(userId)

    override fun saveAll(agreements: List<UserTermAgreement>): List<UserTermAgreement> =
        jpaRepository.saveAll(agreements)
}
