package com.neki.support.application.dto

import com.neki.support.enums.Platform

/**
 * fileName       : AppVersionQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : App version query
 */
object AppVersionQuery {
    data class GetAppVersion(val platform: Platform)
}
