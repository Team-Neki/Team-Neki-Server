package com.neki.user.domain.enums

/**
 * fileName       : Platform
 * author         : darren
 * date           : 2026. 2. 4
 * description    :
 */
enum class Platform(val value: String) {
    ANDROID("android"),
    IOS("ios"),
    ;

    companion object {
        fun from(value: String?): Platform = Platform.entries.firstOrNull { it.value == value }
            ?: ANDROID
        // TODO 안드로이드 심사 끝나면 해당 예외로 변경?: throw BusinessException(ResultCode.INVALID_PARAMETER)
    }
}
