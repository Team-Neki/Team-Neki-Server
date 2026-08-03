package com.neki.support.infra.persist.jpa

import com.neki.support.models.Term
import com.neki.support.models.TermType
import org.springframework.data.jpa.repository.JpaRepository

interface JpaTermRepository : JpaRepository<Term, Long> {

    fun findAllByIsActiveTrueOrderByDisplayOrderAsc(): List<Term>

    fun findAllByIsActiveTrueAndIsRequiredTrueOrderByDisplayOrderAsc(): List<Term>

    fun findByIsActiveTrueAndTermType(termType: TermType): Term?
}
