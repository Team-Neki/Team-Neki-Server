package com.neki.support.repository

import com.neki.support.models.Term
import com.neki.support.models.TermType

interface TermRepository {
    fun findAllActiveTerms(): List<Term>

    fun findAllActiveRequiredTerms(): List<Term>

    fun findActiveByTermType(termType: TermType): Term?

    fun findById(id: Long): Term?

    fun findAllByIds(ids: List<Long>): List<Term>
}
