package com.neki.domain.user.models

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

        /**
         * providerType 을 고려해 Platform 을 결정한다.
         * Apple 로그인은 iOS 에서만 가능하므로, iOS 측에서 platform 값을 보내지 않아도 IOS 로 처리한다.
         */
        fun from(value: String?, providerType: ProviderType): Platform = when {
            value != null -> from(value)
            providerType == ProviderType.APPLE -> IOS
            else -> from(value)
        }
    }
}
