package com.neki.user.infra.client

import com.neki.support.application.command.CheckLatestTermsAgreementCommand
import com.neki.support.application.result.CheckLatestTermsAgreementResult
import com.neki.support.application.usecase.CheckLatestTermsAgreementUseCase
import com.neki.user.application.port.TermClientPort
import org.springframework.stereotype.Component

@Component
class UserTermClient(private val checkLatestTermsAgreementUseCase: CheckLatestTermsAgreementUseCase) : TermClientPort {

    override fun hasAgreedToLatestTerms(userId: Long): Boolean {
        val result: CheckLatestTermsAgreementResult = checkLatestTermsAgreementUseCase.execute(
            CheckLatestTermsAgreementCommand(userId = userId),
        )
        return result.hasAgreedToLatestTerms
    }
}
