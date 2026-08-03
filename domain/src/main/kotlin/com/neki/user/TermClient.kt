package com.neki.user

import com.neki.user.models.TermAgreementStatus

interface TermClient {

    /**
     * 필수 약관 / 마케팅 약관 동의 여부 조회
     */
    fun getAgreementStatus(userId: Long): TermAgreementStatus

    fun revokeOptionalTerms(userId: Long)
}
