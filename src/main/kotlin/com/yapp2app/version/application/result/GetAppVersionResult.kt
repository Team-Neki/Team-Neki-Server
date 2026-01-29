package com.yapp2app.version.application.result

import com.yapp2app.version.domain.enums.Platform

data class GetAppVersionResult(val platform: Platform, val minVersion: String, val currentVersion: String)
