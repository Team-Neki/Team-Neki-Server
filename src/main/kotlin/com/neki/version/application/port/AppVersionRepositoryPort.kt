package com.neki.version.application.port

import com.neki.version.domain.entity.AppVersion
import com.neki.version.domain.enums.Platform

/**
 * fileName       : AppVersionRepositoryPort
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
interface AppVersionRepositoryPort {

    fun findByPlatform(platform: Platform): AppVersion?

    fun save(appVersion: AppVersion): AppVersion
}
