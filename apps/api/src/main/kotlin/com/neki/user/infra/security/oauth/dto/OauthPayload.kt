package com.neki.user.infra.security.oauth.dto

/**
 * fileName       : OauthPayload
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : 외부 OIDC API 응답 payload
 */
data class OIDCPublicKeysPayload(val keys: MutableList<OIDCPublicKey>)

data class OIDCPublicKey(val kid: String, val alg: String, val use: String, val n: String, val e: String)
