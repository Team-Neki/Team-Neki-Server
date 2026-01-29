package com.yapp2app.version.application.command

import com.yapp2app.version.domain.enums.Platform

data class GetAppVersionCommand(val platform: Platform)

data class UpdateAppVersionCommand(val platform: Platform, val minVersion: String, val currentVersion: String)
