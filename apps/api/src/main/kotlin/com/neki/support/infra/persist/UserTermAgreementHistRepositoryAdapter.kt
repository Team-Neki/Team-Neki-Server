package com.neki.support.infra.persist

import com.neki.support.application.port.UserTermAgreementHistRepositoryPort
import com.neki.support.entity.UserTermAgreementHist
import com.neki.support.infra.persist.jpa.JpaUserTermAgreementHistRepository
import org.springframework.stereotype.Repository

@Repository
class UserTermAgreementHistRepositoryAdapter(private val jpaRepository: JpaUserTermAgreementHistRepository) :
    UserTermAgreementHistRepositoryPort {

    override fun saveAll(hists: List<UserTermAgreementHist>) {
        if (hists.isEmpty()) return
        jpaRepository.saveAll(hists)
    }
}
