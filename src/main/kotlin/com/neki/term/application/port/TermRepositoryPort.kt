package com.neki.term.application.port

import com.neki.term.domain.entity.Term

interface TermRepositoryPort {
    fun findAllActiveTerms(): List<Term>

    fun findById(id: Long): Term?

    fun findAllByIds(ids: List<Long>): List<Term>
}
