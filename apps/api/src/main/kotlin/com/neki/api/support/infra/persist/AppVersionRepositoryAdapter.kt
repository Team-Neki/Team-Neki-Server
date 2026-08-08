package com.neki.api.support.infra.persist

import com.neki.api.support.infra.persist.jpa.JpaAppVersionRepository
import com.neki.domain.support.models.AppVersion
import com.neki.domain.support.models.Platform
import com.neki.domain.support.repository.AppVersionRepository
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
