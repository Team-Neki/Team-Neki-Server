package com.neki.api.support.application.dto

import com.neki.domain.support.models.Term

/**
 * fileName       : TermAssembler
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 약관을 응답 항목으로 조립한다.
 */
object TermAssembler {

    fun toTermInfos(terms: List<Term>): List<TermResult.TermInfo> = terms.map { term ->
        TermResult.TermInfo(
            id = term.id!!,
            termType = term.termType,
            title = term.title,
            url = term.url,
            isRequired = term.isRequired,
        )
    }
}
