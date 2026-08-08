package com.neki.api.support.infra.persist.jpa

import com.neki.domain.support.models.UserTermAgreementHist
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserTermAgreementHistRepository : JpaRepository<UserTermAgreementHist, Long>
