package com.neki.user.infra.client

import com.neki.support.application.command.CheckLatestTermsAgreementCommand
import com.neki.support.application.result.CheckLatestTermsAgreementResult
import com.neki.support.application.usecase.CheckLatestTermsAgreementUseCase
import com.neki.support.application.usecase.CheckMarketingAgreementUseCase
import com.neki.support.application.usecase.RevokeOptionalTermsUseCase
import com.neki.user.application.port.TermClientPort
import org.springframework.stereotype.Component

@Component
class UserTermClient(
    private val checkLatestTermsAgreementUseCase: CheckLatestTermsAgreementUseCase,
    private val checkMarketingAgreementUseCase: CheckMarketingAgreementUseCase,
    private val revokeOptionalTermsUseCase: RevokeOptionalTermsUseCase,
) : TermClientPort {

    /**
     * 필수 약관 동의 여부 조회
     */
    override fun hasAgreedToLatestTerms(userId: Long): Boolean {
        val result: CheckLatestTermsAgreementResult = checkLatestTermsAgreementUseCase.execute(
            CheckLatestTermsAgreementCommand(userId = userId),
        )
        return result.hasAgreedToLatestTerms
    }

    /**
     * 마케팅 수신 동의 여부 조회
     */
    override fun hasAgreedToMarketing(userId: Long): Boolean = checkMarketingAgreementUseCase.execute(userId)

    override fun revokeOptionalTerms(userId: Long) = revokeOptionalTermsUseCase.execute(userId)
}
