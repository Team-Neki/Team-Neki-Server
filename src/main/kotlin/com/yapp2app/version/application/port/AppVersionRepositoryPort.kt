package com.yapp2app.version.application.port

import com.yapp2app.version.domain.entity.AppVersion
import com.yapp2app.version.domain.enums.Platform

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
