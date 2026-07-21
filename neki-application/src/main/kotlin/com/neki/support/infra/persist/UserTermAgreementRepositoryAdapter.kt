package com.neki.support.infra.persist

import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.entity.UserTermAgreement
import com.neki.support.infra.persist.jpa.JpaUserTermAgreementRepository
import org.springframework.stereotype.Repository

@Repository
class UserTermAgreementRepositoryAdapter(private val jpaRepository: JpaUserTermAgreementRepository) :
    UserTermAgreementRepositoryPort {

    override fun findByUserId(userId: Long): List<UserTermAgreement> = jpaRepository.findAllByIdUserId(userId)

    override fun findByUserIdAndTermId(userId: Long, termId: Long): UserTermAgreement? =
        jpaRepository.findByIdUserIdAndIdTermId(userId, termId)

    override fun saveAll(agreements: List<UserTermAgreement>): List<UserTermAgreement> =
        jpaRepository.saveAll(agreements)

    override fun save(agreement: UserTermAgreement): UserTermAgreement = jpaRepository.save(agreement)

    override fun deleteAllByUserIdAndTermIds(userId: Long, termIds: List<Long>) {
        if (termIds.isEmpty()) return
        jpaRepository.deleteAllByIdUserIdAndIdTermIdIn(userId, termIds)
    }
}
