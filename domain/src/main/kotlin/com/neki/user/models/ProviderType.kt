package com.neki.user.models

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException

enum class ProviderType(val value: String) {
    LOCAL("local"),
    APPLE("apple"),
    KAKAO("kakao"),
    TEST("test"),
    ;

    companion object {
        fun from(value: String): ProviderType = ProviderType.entries.firstOrNull { it.value == value }
            ?: throw BusinessException(ResultCode.INVALID_PARAMETER)
    }
}
