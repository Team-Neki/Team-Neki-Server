package com.yapp2app.term.infra.persist.jpa

import com.yapp2app.term.domain.entity.Term
import org.springframework.data.jpa.repository.JpaRepository

interface JpaTermRepository : JpaRepository<Term, Long> {

    fun findAllByIsActiveTrueOrderByDisplayOrderAsc(): List<Term>
}
