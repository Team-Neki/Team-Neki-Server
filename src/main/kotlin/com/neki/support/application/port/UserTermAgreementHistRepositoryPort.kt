package com.neki.support.application.port

import com.neki.support.domain.entity.UserTermAgreementHist

interface UserTermAgreementHistRepositoryPort {
    fun saveAll(hists: List<UserTermAgreementHist>)
}
