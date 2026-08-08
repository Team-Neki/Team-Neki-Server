package com.neki.api.support.infra.persist

import com.neki.api.support.infra.persist.jpa.JpaTermRepository
import com.neki.domain.support.models.Term
import com.neki.domain.support.models.TermType
import com.neki.domain.support.repository.TermRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class TermRepositoryAdapter(private val jpaRepository: JpaTermRepository) : TermRepository {

    override fun findAllActiveTerms(): List<Term> = jpaRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()

    override fun findAllActiveRequiredTerms(): List<Term> =
        jpaRepository.findAllByIsActiveTrueAndIsRequiredTrueOrderByDisplayOrderAsc()

    override fun findActiveByTermType(termType: TermType): Term? = jpaRepository.findByIsActiveTrueAndTermType(termType)

    override fun findById(id: Long): Term? = jpaRepository.findByIdOrNull(id)

    override fun findAllByIds(ids: List<Long>): List<Term> = jpaRepository.findAllById(ids)
}
