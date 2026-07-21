package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.application.dto.TermCommand
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementHistRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.domain.entity.Term
import com.neki.support.domain.entity.UserTermAgreement
import com.neki.support.domain.entity.UserTermAgreementHist
import com.neki.support.domain.enums.TermAgreementAction
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@UseCase
class CreateTermAgreementsUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
    private val userTermAgreementHistRepository: UserTermAgreementHistRepositoryPort,
) {

    @Transactional
    fun execute(command: TermCommand.CreateTermAgreements) {
        val activeTermsById: Map<Long, Term> = termRepository.findAllActiveTerms()
            .associateBy { it.id!! }

        validateRequestedTermsAreActive(command.agreements, activeTermsById)

        val now: LocalDateTime = LocalDateTime.now()
        processRequiredTerms(command, activeTermsById, now)
        processOptionalTerms(command, activeTermsById, now)
    }

    /**
     * 요청에 포함된 약관이 모두 현재 활성화된 약관인지 검증한다.
     * 존재하지 않는 약관이 포함되면 예외를 던진다.
     */
    private fun validateRequestedTermsAreActive(
        agreements: List<TermCommand.TermAgreementItem>,
        activeTermsById: Map<Long, Term>,
    ) {
        val hasUnknownTerm: Boolean = agreements.any { it.termId !in activeTermsById }
        if (hasUnknownTerm) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }
    }

    /**
     * 필수 약관 동의를 처리한다.
     *
     * - 이미 모든 필수 약관에 동의한 사용자라면 추가 처리하지 않는다.
     * - 아직 동의하지 않은 사용자가 요청에 필수 약관 동의를 모두 포함했다면(= 최초 가입) 동의 처리한다.
     * - 그 외(아직 동의하지 않았는데 요청에 필수 약관이 빠져 있음)에는 예외를 던진다.
     */
    private fun processRequiredTerms(
        command: TermCommand.CreateTermAgreements,
        activeTermsById: Map<Long, Term>,
        now: LocalDateTime,
    ) {
        val requiredTerms: List<Term> = activeTermsById.values.filter { it.isRequired }

        val alreadyAgreedTermIds: Set<Long> = userTermAgreementRepository.findByUserId(command.userId)
            .map { it.id.termId }
            .toSet()
        val isAlreadyAgreedToAllRequired: Boolean = requiredTerms.all { it.id in alreadyAgreedTermIds }
        if (isAlreadyAgreedToAllRequired) {
            return
        }

        val requestedAgreedTermIds: Set<Long> = command.agreements
            .filter { it.agreed }
            .map { it.termId }
            .toSet()
        val isAllRequiredAgreedInRequest: Boolean = requiredTerms.all { it.id in requestedAgreedTermIds }
        if (!isAllRequiredAgreedInRequest) {
            throw BusinessException(ResultCode.REQUIRED_TERMS_NOT_AGREED)
        }

        val termsToAgree: List<Term> = requiredTerms.filter { it.id !in alreadyAgreedTermIds }
        userTermAgreementRepository.saveAll(termsToAgree.toAgreements(command.userId, now))
    }

    private fun processOptionalTerms(
        command: TermCommand.CreateTermAgreements,
        activeTermsById: Map<Long, Term>,
        now: LocalDateTime,
    ) {
        val optionalItems: List<TermCommand.TermAgreementItem> = command.agreements
            .filterNot { activeTermsById.getValue(it.termId).isRequired }
        val (agreedItems, disagreedItems) = optionalItems.partition { it.agreed }

        val existingAgreedTermIds: Set<Long> = userTermAgreementRepository.findByUserId(command.userId)
            .map { it.id.termId }
            .toSet()

        val termsToDisagree: List<Long> = disagreedItems.map { it.termId }
        userTermAgreementRepository.deleteAllByUserIdAndTermIds(command.userId, termsToDisagree)

        val termsToAgree: List<Term> = agreedItems
            .filter { it.termId !in existingAgreedTermIds }
            .map { activeTermsById.getValue(it.termId) }
        userTermAgreementRepository.saveAll(termsToAgree.toAgreements(command.userId, now))

        val hists: List<UserTermAgreementHist> =
            agreedItems.filter { it.termId !in existingAgreedTermIds }.map {
                UserTermAgreementHist(userId = command.userId, termId = it.termId, action = TermAgreementAction.AGREED)
            } +
                disagreedItems.filter { it.termId in existingAgreedTermIds }.map {
                    UserTermAgreementHist(
                        userId = command.userId,
                        termId = it.termId,
                        action = TermAgreementAction.WITHDRAWN,
                    )
                }
        userTermAgreementHistRepository.saveAll(hists)
    }

    private fun List<Term>.toAgreements(userId: Long, agreedAt: LocalDateTime): List<UserTermAgreement> = map { term ->
        UserTermAgreement(
            userId = userId,
            termId = term.id!!,
            agreedAt = agreedAt,
            termVersion = term.version,
        )
    }
}
