package com.neki.domain.support.models

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException

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
