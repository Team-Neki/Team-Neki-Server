package com.neki.user.infra.client

import com.neki.support.application.GetTermAgreementStatusUseCase
import com.neki.support.application.RevokeOptionalTermsUseCase
import com.neki.support.application.dto.TermResult
import com.neki.support.dto.TermCommand
import com.neki.support.dto.TermQuery
import com.neki.user.client.TermClient
import com.neki.user.models.TermAgreementStatus
import org.springframework.stereotype.Component

@Component
class UserTermClient(
    private val getTermAgreementStatusUseCase: GetTermAgreementStatusUseCase,
    private val revokeOptionalTermsUseCase: RevokeOptionalTermsUseCase,
) : TermClient {

    /**
     * 필수 약관 / 마케팅 약관 동의 여부 조회
     */
    override fun getAgreementStatus(userId: Long): TermAgreementStatus {
        val result: TermResult.AgreementStatus = getTermAgreementStatusUseCase.execute(
            TermQuery.GetAgreementStatus(userId = userId),
        )

        return TermAgreementStatus(
            requiredAgreed = result.requiredAgreed,
            marketingAgreed = result.marketingAgreed,
        )
    }

    override fun revokeOptionalTerms(userId: Long) =
        revokeOptionalTermsUseCase.execute(TermCommand.RevokeOptionalTerms(userId))
}
