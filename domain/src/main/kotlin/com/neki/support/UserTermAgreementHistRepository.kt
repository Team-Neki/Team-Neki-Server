package com.neki.support

import com.neki.support.models.UserTermAgreementHist

interface UserTermAgreementHistRepository {
    fun saveAll(hists: List<UserTermAgreementHist>)
}
