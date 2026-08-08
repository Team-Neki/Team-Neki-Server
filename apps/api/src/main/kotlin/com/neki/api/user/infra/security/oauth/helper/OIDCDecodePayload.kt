package com.neki.api.user.infra.security.oauth.helper

/**
 * fileName       : OIDCDecodePayload
 * author         : koo
 * date           : 2026. 7. 22.
 * description    : idToken 디코딩 결과 (OAuth 어댑터 내부 타입)
 */
data class OIDCDecodePayload(
    /** issuer ex https://kauth.kakao.com  */
    val iss: String,
    /** client id  */
    val aud: String,
    /** oauth provider account unique id  */
    val sub: String,
    /** biz 앱 신청을 해야 email을 수집가능,,  */
    val email: String?,
    /** 닉네임  */
    val nickname: String?,
    /** 프로필 이미지  */
    val imageUrl: String?,
)
