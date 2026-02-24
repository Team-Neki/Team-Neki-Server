package com.neki.version.infra.persist.jpa

import com.neki.version.domain.entity.AppVersion
import com.neki.version.domain.enums.Platform
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
