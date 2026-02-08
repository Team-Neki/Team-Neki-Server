package com.yapp2app.term.infra.persist

import com.yapp2app.term.application.port.TermRepositoryPort
import com.yapp2app.term.domain.entity.Term
import com.yapp2app.term.infra.persist.jpa.JpaTermRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class TermRepositoryAdapter(private val jpaRepository: JpaTermRepository) : TermRepositoryPort {

    override fun findAllActiveTerms(): List<Term> = jpaRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()

    override fun findById(id: Long): Term? = jpaRepository.findByIdOrNull(id)

    override fun findAllByIds(ids: List<Long>): List<Term> = jpaRepository.findAllById(ids)
}
