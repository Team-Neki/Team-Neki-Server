package com.yapp2app.user.infra.client

import com.yapp2app.term.application.command.CheckLatestTermsAgreementCommand
import com.yapp2app.term.application.usecase.CheckLatestTermsAgreementUseCase
import com.yapp2app.user.application.port.TermClientPort
import org.springframework.stereotype.Component

@Component
class UserTermClient(private val checkLatestTermsAgreementUseCase: CheckLatestTermsAgreementUseCase) : TermClientPort {

    override fun hasAgreedToLatestTerms(userId: Long): Boolean {
        val result = checkLatestTermsAgreementUseCase.execute(
            CheckLatestTermsAgreementCommand(userId = userId),
        )
        return result.hasAgreedToLatestTerms
    }
}
