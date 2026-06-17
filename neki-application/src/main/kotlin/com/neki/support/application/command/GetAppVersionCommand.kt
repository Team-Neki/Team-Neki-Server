package com.neki.support.application.command

import com.neki.support.enums.Platform

/**
 * fileName       : VersionCommand
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
data class GetAppVersionCommand(val platform: Platform)

data class UpdateAppVersionCommand(val platform: Platform, val minVersion: String, val currentVersion: String)
