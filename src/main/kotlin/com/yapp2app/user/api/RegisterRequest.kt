package com.yapp2app.user.api

/**
 * fileName       : UserRequest
 * author         : koo
 * date           : 2025. 12. 28. 오후 7:45
 * description    : 로컬 회원가입을 위한 요청 dto
 */
data class RegisterRequest(val email: String, val name: String, val password: String)
