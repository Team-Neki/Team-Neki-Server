package com.neki.domain.support.repository

import com.neki.domain.support.models.UserTermAgreementHist

interface UserTermAgreementHistRepository {
    fun saveAll(hists: List<UserTermAgreementHist>)
}
