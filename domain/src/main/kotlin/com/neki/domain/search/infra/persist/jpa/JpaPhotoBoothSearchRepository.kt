package com.neki.domain.search.infra.persist.jpa

import com.neki.domain.search.models.PhotoBoothSearch
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaPhotoBoothSearchRepository
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : PhotoBoothSearch(_read) JPA Repository. 읽기 전용
 */
interface JpaPhotoBoothSearchRepository : JpaRepository<PhotoBoothSearch, Long>
