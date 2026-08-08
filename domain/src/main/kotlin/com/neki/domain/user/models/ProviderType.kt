package com.neki.domain.user.models

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException

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
