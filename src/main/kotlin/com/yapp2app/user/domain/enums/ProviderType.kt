package com.yapp2app.user.domain.enums

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException

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
