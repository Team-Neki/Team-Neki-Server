package com.neki.support.application.port

import com.neki.support.domain.entity.Term

interface TermRepositoryPort {
    fun findAllActiveTerms(): List<Term>

    fun findById(id: Long): Term?

    fun findAllByIds(ids: List<Long>): List<Term>
}
