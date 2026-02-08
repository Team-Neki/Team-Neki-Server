package com.yapp2app.term.application.port

import com.yapp2app.term.domain.entity.Term

interface TermRepositoryPort {
    fun findAllActiveTerms(): List<Term>

    fun findById(id: Long): Term?

    fun findAllByIds(ids: List<Long>): List<Term>
}
