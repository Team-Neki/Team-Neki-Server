package com.neki.support.application.port

import com.neki.support.domain.entity.Term
import com.neki.support.domain.enums.TermType

interface TermRepositoryPort {
    fun findAllActiveTerms(): List<Term>

    fun findAllActiveRequiredTerms(): List<Term>

    fun findActiveByTermType(termType: TermType): Term?

    fun findById(id: Long): Term?

    fun findAllByIds(ids: List<Long>): List<Term>
}
