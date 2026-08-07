package com.neki.support.repository

import com.neki.support.models.AppVersion
import com.neki.support.models.Platform

/**
 * fileName       : AppVersionRepository
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
interface AppVersionRepository {

    fun findByPlatform(platform: Platform): AppVersion?

    fun save(appVersion: AppVersion): AppVersion
}
