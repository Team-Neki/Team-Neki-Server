package com.yapp2app.auth.infra.response

import com.yapp2app.user.domain.enums.ProviderType

/**
 * fileName       : KakaoClientResponse
 * author         : darren
 * date           : 2025. 12. 29. 14:23
 * description    : 카카오 외부 통신 과정에서 변환된 DTO
 */

/**
 * 카카오 사용자정보 추출 DTO
 */
data class OauthInfoResponse(val providerType: ProviderType, val oid: Long, val email: String?, val name: String?, val imageUrl: String?)

data class OIDCDecodePayloadResponse(
    /** issuer ex https://kauth.kakao.com  */
    val iss: String,
    /** client id  */
    val aud: String,
    /** oauth provider account unique id  */
    val sub: Long,
    /** biz 앱 신청을 해야 email을 수집가능,,  */
    val email: String?,
    /** 닉네임  */
    val nickname: String?,
    /** 프로필 이미지  */
    val imageUrl: String?
)

data class OIDCPublicKeysResponse(var keys: MutableList<OIDCPublicKeyDto>)

data class OIDCPublicKeyDto(val kid: String, val alg: String, val use: String, val n: String, val e: String)

/**
 * fileName       : AuthResult
 * author         : darren
 * date           : 2025. 12. 26. 18:20
 * description    : Auth usercase 관련 result idToken을 얻기 위한 테스트 DTO
 */
data class GetKakaoTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val refreshToken: String,
    val expiresIn: Int,
    val scope: String? = null,
    val refreshTokenExpiresIn: Int? = null,
    val idToken: String? = null,
)
