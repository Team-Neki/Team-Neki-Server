package com.yapp2app.user.application.command

import com.yapp2app.user.domain.enums.ProviderType

/**
 * fileName       : RegisterCommand
 * author         : koo
 * date           : 2025. 12. 28. 오후 7:41
 * description    : 로컬 회원가입을 위한 command
 */
data class RegisterCommand(val email: String, val name: String, val password: String, val providerType: ProviderType)
