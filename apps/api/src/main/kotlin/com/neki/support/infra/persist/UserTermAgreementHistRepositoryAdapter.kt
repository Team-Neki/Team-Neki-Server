package com.neki.support.infra.persist

import com.neki.support.UserTermAgreementHistRepository
import com.neki.support.infra.persist.jpa.JpaUserTermAgreementHistRepository
import com.neki.support.models.UserTermAgreementHist
import org.springframework.stereotype.Repository

@Repository
class UserTermAgreementHistRepositoryAdapter(private val jpaRepository: JpaUserTermAgreementHistRepository) :
    UserTermAgreementHistRepository {

    override fun saveAll(hists: List<UserTermAgreementHist>) {
        if (hists.isEmpty()) return
        jpaRepository.saveAll(hists)
    }
}
