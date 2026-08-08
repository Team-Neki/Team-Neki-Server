package com.neki.api.support.infra.persist.jpa

import com.neki.domain.support.models.Term
import com.neki.domain.support.models.TermType
import org.springframework.data.jpa.repository.JpaRepository

interface JpaTermRepository : JpaRepository<Term, Long> {

    fun findAllByIsActiveTrueOrderByDisplayOrderAsc(): List<Term>

    fun findAllByIsActiveTrueAndIsRequiredTrueOrderByDisplayOrderAsc(): List<Term>

    fun findByIsActiveTrueAndTermType(termType: TermType): Term?
}
