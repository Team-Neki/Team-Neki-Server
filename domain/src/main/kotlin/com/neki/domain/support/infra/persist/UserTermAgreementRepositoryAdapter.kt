package com.neki.domain.support.infra.persist

import com.neki.domain.support.infra.persist.jpa.JpaUserTermAgreementRepository
import com.neki.domain.support.models.UserTermAgreement
import com.neki.domain.support.repository.UserTermAgreementRepository
import org.springframework.stereotype.Repository

@Repository
class UserTermAgreementRepositoryAdapter(private val jpaRepository: JpaUserTermAgreementRepository) :
    UserTermAgreementRepository {

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
