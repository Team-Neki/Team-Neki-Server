package com.neki.api.support.application

import com.neki.api.support.application.dto.TermResult
import com.neki.core.annotation.UseCase
import com.neki.domain.support.dto.TermQuery
import com.neki.domain.support.models.TermAgreementStatus
import com.neki.domain.support.service.TermService

/**
 * fileName       : GetTermAgreementStatusUseCase
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : 필수 약관 / 마케팅 약관 동의 여부를 한 번에 조회
 */
@UseCase
class GetTermAgreementStatusUseCase(private val termService: TermService) {

    fun execute(query: TermQuery.GetAgreementStatus): TermResult.AgreementStatus {
        val status: TermAgreementStatus = termService.getAgreementStatus(query)

        return TermResult.AgreementStatus(
            requiredAgreed = status.requiredAgreed,
            marketingAgreed = status.marketingAgreed,
        )
    }
}
