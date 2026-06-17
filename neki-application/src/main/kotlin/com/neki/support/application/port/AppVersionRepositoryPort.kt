package com.neki.support.application.port

import com.neki.support.domain.entity.AppVersion
import com.neki.support.domain.enums.Platform

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
