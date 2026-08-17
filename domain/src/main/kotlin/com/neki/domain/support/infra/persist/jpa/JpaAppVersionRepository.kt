package com.neki.domain.support.infra.persist.jpa

import com.neki.domain.support.models.AppVersion
import com.neki.domain.support.models.Platform
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaAppVersionRepository
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
interface JpaAppVersionRepository : JpaRepository<AppVersion, Long> {

    fun findByPlatform(platform: Platform): AppVersion?
}
