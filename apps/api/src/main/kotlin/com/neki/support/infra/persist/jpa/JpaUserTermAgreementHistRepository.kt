package com.neki.support.infra.persist.jpa

import com.neki.support.models.UserTermAgreementHist
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserTermAgreementHistRepository : JpaRepository<UserTermAgreementHist, Long>
