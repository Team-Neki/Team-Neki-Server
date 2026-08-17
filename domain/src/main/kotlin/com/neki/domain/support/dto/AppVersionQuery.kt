package com.neki.domain.support.dto

import com.neki.domain.support.models.Platform

/**
 * fileName       : AppVersionQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : App version query
 */
object AppVersionQuery {
    data class GetAppVersion(val platform: Platform)
}
