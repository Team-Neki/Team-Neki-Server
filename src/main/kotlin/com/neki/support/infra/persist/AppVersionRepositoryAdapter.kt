package com.neki.support.infra.persist

import com.neki.support.application.port.AppVersionRepositoryPort
import com.neki.support.domain.entity.AppVersion
import com.neki.support.domain.enums.Platform
import com.neki.support.infra.persist.jpa.JpaAppVersionRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : AppVersionRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
@Repository
class AppVersionRepositoryAdapter(private val jpaRepository: JpaAppVersionRepository) : AppVersionRepositoryPort {

    override fun findByPlatform(platform: Platform): AppVersion? = jpaRepository.findByPlatform(platform)

    override fun save(appVersion: AppVersion): AppVersion = jpaRepository.save(appVersion)
}
