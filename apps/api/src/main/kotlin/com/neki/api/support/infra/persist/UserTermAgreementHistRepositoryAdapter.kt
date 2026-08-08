package com.neki.api.support.infra.persist

import com.neki.api.support.infra.persist.jpa.JpaUserTermAgreementHistRepository
import com.neki.domain.support.models.UserTermAgreementHist
import com.neki.domain.support.repository.UserTermAgreementHistRepository
import org.springframework.stereotype.Repository

@Repository
class UserTermAgreementHistRepositoryAdapter(private val jpaRepository: JpaUserTermAgreementHistRepository) :
    UserTermAgreementHistRepository {

    override fun saveAll(hists: List<UserTermAgreementHist>) {
        if (hists.isEmpty()) return
        jpaRepository.saveAll(hists)
    }
}
