package com.neki.support.infra.persist

import com.neki.support.AppVersionRepository
import com.neki.support.infra.persist.jpa.JpaAppVersionRepository
import com.neki.support.models.AppVersion
import com.neki.support.models.Platform
import org.springframework.stereotype.Repository

/**
 * fileName       : AppVersionRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
@Repository
class AppVersionRepositoryAdapter(private val jpaRepository: JpaAppVersionRepository) : AppVersionRepository {

    override fun findByPlatform(platform: Platform): AppVersion? = jpaRepository.findByPlatform(platform)

    override fun save(appVersion: AppVersion): AppVersion = jpaRepository.save(appVersion)
}
