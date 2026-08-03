package com.neki.user.models

/**
 * fileName       : OauthRegistration
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : OAuth 로그인 시 조회/가입된 사용자와 신규 가입 여부
 */
data class OauthRegistration(val user: User, val isNew: Boolean)
