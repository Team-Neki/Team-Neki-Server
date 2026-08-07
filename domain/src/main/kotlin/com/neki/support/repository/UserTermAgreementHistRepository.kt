package com.neki.support.repository

import com.neki.support.models.UserTermAgreementHist

interface UserTermAgreementHistRepository {
    fun saveAll(hists: List<UserTermAgreementHist>)
}
