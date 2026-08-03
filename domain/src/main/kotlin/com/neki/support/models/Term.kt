package com.neki.support.models

import com.neki.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "TB_TERM")
class Term(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "term_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val termType: TermType,

    @Column(name = "title", nullable = false, length = 100)
    val title: String,

    @Column(name = "url", nullable = false, length = 500)
    val url: String,

    @Column(name = "version", nullable = false, length = 20)
    val version: String,

    @Column(name = "is_required", nullable = false)
    val isRequired: Boolean = true,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int = 0,
) : BaseTimeEntity()
