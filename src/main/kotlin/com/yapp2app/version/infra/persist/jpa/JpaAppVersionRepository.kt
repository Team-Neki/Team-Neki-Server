package com.yapp2app.version.infra.persist.jpa

import com.yapp2app.version.domain.entity.AppVersion
import com.yapp2app.version.domain.enums.Platform
import org.springframework.data.jpa.repository.JpaRepository

interface JpaAppVersionRepository : JpaRepository<AppVersion, Long> {

    fun findByPlatform(platform: Platform): AppVersion?
}
