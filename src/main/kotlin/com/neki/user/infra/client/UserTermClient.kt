package com.neki.user.infra.client

import com.neki.support.application.command.CheckRequiredTermsAgreementCommand
import com.neki.support.application.result.TermAgreementResult
import com.neki.support.application.usecase.CheckMarketingAgreementUseCase
import com.neki.support.application.usecase.CheckRequiredTermsAgreementUseCase
import com.neki.support.application.usecase.RevokeOptionalTermsUseCase
import com.neki.user.application.port.TermClientPort
import org.springframework.stereotype.Component

@Component
class UserTermClient(
    private val checkRequiredTermsAgreementUseCase: CheckRequiredTermsAgreementUseCase,
    private val checkMarketingAgreementUseCase: CheckMarketingAgreementUseCase,
    private val revokeOptionalTermsUseCase: RevokeOptionalTermsUseCase,
) : TermClientPort {

    /**
     * 필수 약관 동의 여부 조회
     */
    override fun hasAgreedToAllRequired(userId: Long): Boolean {
        val result: TermAgreementResult = checkRequiredTermsAgreementUseCase.execute(
            CheckRequiredTermsAgreementCommand(userId = userId),
        )
        return result.agreed
    }

    /**
     * 마케팅 수신 동의 여부 조회
     */
    override fun hasAgreedToMarketing(userId: Long): Boolean = checkMarketingAgreementUseCase.execute(userId).agreed

    override fun revokeOptionalTerms(userId: Long) = revokeOptionalTermsUseCase.execute(userId)
}
