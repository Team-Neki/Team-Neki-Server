package com.neki.api.user.infra.client

import com.neki.api.support.application.GetTermAgreementStatusUseCase
import com.neki.api.support.application.RevokeOptionalTermsUseCase
import com.neki.api.support.application.dto.TermResult
import com.neki.domain.support.dto.TermCommand
import com.neki.domain.support.dto.TermQuery
import com.neki.domain.user.client.TermClient
import com.neki.domain.user.models.TermAgreementStatus
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
