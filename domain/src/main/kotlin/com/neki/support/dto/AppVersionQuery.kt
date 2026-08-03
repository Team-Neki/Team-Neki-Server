package com.neki.support.dto

import com.neki.support.models.Platform

/**
 * fileName       : AppVersionQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : App version query
 */
object AppVersionQuery {
    data class GetAppVersion(val platform: Platform)
}
