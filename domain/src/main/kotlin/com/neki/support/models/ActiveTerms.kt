package com.neki.support.models

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException

/**
 * fileName       : ActiveTerms
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 현재 활성화된 약관 묶음. 약관 판정은 모두 이 묶음에 물어본다.
 */
class ActiveTerms(private val terms: List<Term>) {

    private val byId: Map<Long, Term> = terms.associateBy { it.id!! }

    val all: List<Term> get() = terms

    val required: List<Term> get() = terms.filter { it.isRequired }

    val optionalIds: Set<Long> get() = terms.filterNot { it.isRequired }.mapNotNull { it.id }.toSet()

    fun isRequired(termId: Long): Boolean = get(termId).isRequired

    /**
     * 활성 약관에 없는 termId가 섞여 있으면 요청 자체가 잘못된 것으로 본다.
     */
    fun validateAllActive(termIds: List<Long>) {
        if (termIds.any { it !in byId }) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }
    }

    fun get(termId: Long): Term = byId.getValue(termId)

    fun marketingTermId(): Long? = terms.firstOrNull { it.termType == TermType.MARKETING }?.id
}
