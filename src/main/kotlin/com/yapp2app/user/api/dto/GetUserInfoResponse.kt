package com.yapp2app.user.api.dto

import com.yapp2app.user.domain.enums.ProviderType

/**
 * fileName       : GetUserInfoResponse
 * author         : darren
 * date           : 2025. 12. 31. 14:45
 * description    :
 */

data class GetUserInfoResponse(val name: String, val providerType: ProviderType)
