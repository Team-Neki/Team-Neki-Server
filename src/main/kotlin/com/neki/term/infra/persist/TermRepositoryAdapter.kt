package com.neki.term.infra.persist

import com.neki.term.application.port.TermRepositoryPort
import com.neki.term.domain.entity.Term
import com.neki.term.infra.persist.jpa.JpaTermRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class TermRepositoryAdapter(private val jpaRepository: JpaTermRepository) : TermRepositoryPort {

    override fun findAllActiveTerms(): List<Term> = jpaRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()

    override fun findById(id: Long): Term? = jpaRepository.findByIdOrNull(id)

    override fun findAllByIds(ids: List<Long>): List<Term> = jpaRepository.findAllById(ids)
}
