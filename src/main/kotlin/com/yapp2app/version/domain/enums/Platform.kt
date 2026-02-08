package com.yapp2app.version.domain.enums

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException

/**
 * fileName       : Platform
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
enum class Platform(val value: String) {
    ANDROID("android"),
    IOS("ios"),
    ;

    companion object {
        fun from(value: String): Platform = Platform.entries.firstOrNull { it.value == value }
            ?: throw BusinessException(ResultCode.INVALID_PARAMETER)
    }
}
