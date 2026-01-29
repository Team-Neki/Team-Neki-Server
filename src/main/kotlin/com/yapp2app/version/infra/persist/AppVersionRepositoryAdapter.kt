package com.yapp2app.version.infra.persist

import com.yapp2app.version.application.port.AppVersionRepositoryPort
import com.yapp2app.version.domain.entity.AppVersion
import com.yapp2app.version.domain.enums.Platform
import com.yapp2app.version.infra.persist.jpa.JpaAppVersionRepository
import org.springframework.stereotype.Repository

@Repository
class AppVersionRepositoryAdapter(private val jpaRepository: JpaAppVersionRepository) : AppVersionRepositoryPort {

    override fun findByPlatform(platform: Platform): AppVersion? = jpaRepository.findByPlatform(platform)

    override fun save(appVersion: AppVersion): AppVersion = jpaRepository.save(appVersion)
}
