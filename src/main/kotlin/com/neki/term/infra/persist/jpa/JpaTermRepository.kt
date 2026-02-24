package com.neki.term.infra.persist.jpa

import com.neki.term.domain.entity.Term
import org.springframework.data.jpa.repository.JpaRepository

interface JpaTermRepository : JpaRepository<Term, Long> {

    fun findAllByIsActiveTrueOrderByDisplayOrderAsc(): List<Term>
}
