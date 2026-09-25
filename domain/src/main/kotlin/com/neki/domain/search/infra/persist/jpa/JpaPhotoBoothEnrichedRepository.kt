package com.neki.domain.search.infra.persist.jpa

import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothEnrichedId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaPhotoBoothEnrichedRepository
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : PhotoBoothEnriched JPA Repository (읽기 전용)
 */
interface JpaPhotoBoothEnrichedRepository : JpaRepository<PhotoBoothEnriched, PhotoBoothEnrichedId>
