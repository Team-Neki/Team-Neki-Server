package com.neki.domain.search.infra.persist.jpa

import com.neki.domain.search.models.LegalDong
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaLegalDongRepository
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : LegalDong JPA Repository
 */
interface JpaLegalDongRepository : JpaRepository<LegalDong, String>
