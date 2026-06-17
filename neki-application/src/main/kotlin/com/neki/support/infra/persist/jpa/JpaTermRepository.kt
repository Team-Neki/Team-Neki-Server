package com.neki.support.infra.persist.jpa

import com.neki.support.entity.Term
import org.springframework.data.jpa.repository.JpaRepository

interface JpaTermRepository : JpaRepository<Term, Long> {

    fun findAllByIsActiveTrueOrderByDisplayOrderAsc(): List<Term>
}
