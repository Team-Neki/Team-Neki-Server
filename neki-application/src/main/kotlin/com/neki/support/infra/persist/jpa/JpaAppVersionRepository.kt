package com.neki.support.infra.persist.jpa

import com.neki.support.domain.entity.AppVersion
import com.neki.support.domain.enums.Platform
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
