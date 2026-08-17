package com.neki.domain.support.repository

import com.neki.domain.support.models.Term
import com.neki.domain.support.models.TermType

interface TermRepository {
    fun findAllActiveTerms(): List<Term>

    fun findAllActiveRequiredTerms(): List<Term>

    fun findActiveByTermType(termType: TermType): Term?

    fun findById(id: Long): Term?

    fun findAllByIds(ids: List<Long>): List<Term>
}
