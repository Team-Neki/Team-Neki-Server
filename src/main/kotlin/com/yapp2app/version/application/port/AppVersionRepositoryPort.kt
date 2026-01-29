package com.yapp2app.version.application.port

import com.yapp2app.version.domain.entity.AppVersion
import com.yapp2app.version.domain.enums.Platform

interface AppVersionRepositoryPort {

    fun findByPlatform(platform: Platform): AppVersion?

    fun save(appVersion: AppVersion): AppVersion
}
