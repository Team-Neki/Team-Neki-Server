package com.neki.domain.support.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.support.dto.TermCommand
import com.neki.domain.support.dto.TermQuery
import com.neki.domain.support.models.ActiveTerms
import com.neki.domain.support.models.Term
import com.neki.domain.support.models.TermAgreementStatus
import com.neki.domain.support.models.UserTermAgreement
import com.neki.domain.support.models.UserTermAgreementHist
import com.neki.domain.support.repository.TermRepository
import com.neki.domain.support.repository.UserTermAgreementHistRepository
import com.neki.domain.support.repository.UserTermAgreementRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * fileName       : TermService
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 약관 도메인 서비스
 */
@Component
class TermService(
    private val termRepository: TermRepository,
    private val userTermAgreementRepository: UserTermAgreementRepository,
    private val userTermAgreementHistRepository: UserTermAgreementHistRepository,
) {

    fun getActiveTerms(): List<Term> = activeTerms().all

    fun createAgreements(command: TermCommand.CreateTermAgreements) {
        val activeTerms: ActiveTerms = activeTerms()

        activeTerms.validateAllActive(command.agreements.map { it.termId })

        val now: LocalDateTime = LocalDateTime.now()
        processRequiredTerms(command, activeTerms, now)
        processOptionalTerms(command, activeTerms, now)
    }

    /**
     * 필수 약관은 동의한 버전이 현재 활성 버전과 같아야 동의로 인정한다.
     * 마케팅 약관은 버전과 무관하게 동의 기록 존재 여부로 판단한다.
     */
    fun getAgreementStatus(query: TermQuery.GetAgreementStatus): TermAgreementStatus {
        val activeTerms: ActiveTerms = activeTerms()
        val userAgreements: List<UserTermAgreement> = userTermAgreementRepository.findByUserId(query.userId)

        val agreedTermVersions: Set<Pair<Long, String>> = userAgreements.map { it.id.termId to it.termVersion }.toSet()
        val agreedTermIds: Set<Long> = userAgreements.map { it.id.termId }.toSet()

        val requiredAgreed: Boolean = activeTerms.required.all { (it.id to it.version) in agreedTermVersions }

        val marketingAgreed: Boolean = activeTerms.marketingTermId()?.let { it in agreedTermIds } ?: false

        return TermAgreementStatus(requiredAgreed = requiredAgreed, marketingAgreed = marketingAgreed)
    }

    fun revokeOptionalTerms(command: TermCommand.RevokeOptionalTerms) {
        val optionalTermIds: Set<Long> = activeTerms().optionalIds

        val agreedOptionalTermIds: List<Long> = userTermAgreementRepository.findByUserId(command.userId)
            .map { it.id.termId }
            .filter { it in optionalTermIds }

        if (agreedOptionalTermIds.isEmpty()) return

        userTermAgreementRepository.deleteAllByUserIdAndTermIds(command.userId, agreedOptionalTermIds)

        val hists: List<UserTermAgreementHist> = agreedOptionalTermIds.map {
            UserTermAgreementHist.withdrawn(command.userId, it)
        }
        userTermAgreementHistRepository.saveAll(hists)
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
        activeTerms: ActiveTerms,
        now: LocalDateTime,
    ) {
        val requiredTerms: List<Term> = activeTerms.required

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
        activeTerms: ActiveTerms,
        now: LocalDateTime,
    ) {
        val optionalItems: List<TermCommand.TermAgreementItem> = command.agreements
            .filterNot { activeTerms.isRequired(it.termId) }
        val (agreedItems, disagreedItems) = optionalItems.partition { it.agreed }

        val existingAgreedTermIds: Set<Long> = userTermAgreementRepository.findByUserId(command.userId)
            .map { it.id.termId }
            .toSet()

        // 이미 동의한 약관을 다시 요청해도 새 동의로 치지 않고, 동의한 적 없는 약관 철회는 이력을 남기지 않는다
        val newlyAgreedItems: List<TermCommand.TermAgreementItem> =
            agreedItems.filter { it.termId !in existingAgreedTermIds }
        val withdrawnItems: List<TermCommand.TermAgreementItem> =
            disagreedItems.filter { it.termId in existingAgreedTermIds }

        userTermAgreementRepository.deleteAllByUserIdAndTermIds(command.userId, disagreedItems.map { it.termId })

        val termsToAgree: List<Term> = newlyAgreedItems.map { activeTerms.get(it.termId) }
        userTermAgreementRepository.saveAll(termsToAgree.toAgreements(command.userId, now))

        val hists: List<UserTermAgreementHist> =
            newlyAgreedItems.map { UserTermAgreementHist.agreed(command.userId, it.termId) } +
                withdrawnItems.map { UserTermAgreementHist.withdrawn(command.userId, it.termId) }
        userTermAgreementHistRepository.saveAll(hists)
    }

    private fun activeTerms(): ActiveTerms = ActiveTerms(termRepository.findAllActiveTerms())

    private fun List<Term>.toAgreements(userId: Long, agreedAt: LocalDateTime): List<UserTermAgreement> = map { term ->
        UserTermAgreement(
            userId = userId,
            termId = term.id!!,
            agreedAt = agreedAt,
            termVersion = term.version,
        )
    }
}
