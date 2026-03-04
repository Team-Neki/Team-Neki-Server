package com.neki.user.infra.security.oauth.helper

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.application.contract.OIDCDecodePayload
import com.neki.user.application.contract.OIDCPublicKeyDto
import com.neki.user.application.contract.OIDCPublicKeysPayload
import com.neki.user.application.contract.OauthInfoPayload
import com.neki.user.domain.enums.Platform
import com.neki.user.domain.enums.ProviderType
import com.neki.user.infra.security.config.OauthProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import kotlin.collections.get

/**
 * fileName       : AppleOauthHelper
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : Apple OIDC 토큰 검증 헬퍼
 *
 * Apple Sign In OIDC 검증 절차:
 * 1. ID 토큰의 온점(.)을 기준으로 헤더, 페이로드, 서명 분리
 * 2. idToken의 kid로 jwks 검증
 * 3. iss 값이 https://appleid.apple.com와 일치하는지 확인
 * 4. aud 값이 앱의 Bundle ID와 일치하는지 확인
 * 5. exp 값이 현재 시간보다 큰지 확인 (만료 여부)
 * 6. 서명 검증 (RS256)
 *
 * 참고:
 * - https://developer.apple.com/documentation/sign_in_with_apple/sign_in_with_apple_rest_api/verifying_a_user
 * - https://appleid.apple.com/.well-known/openid-configuration
 */
@Component
class AppleOauthHelper(private val oauthProperties: OauthProperties, private val objectMapper: ObjectMapper) :
    OauthHelper {

    companion object {
        private const val HEADER_KID = "kid"
        private const val CLAIM_EMAIL = "email"
        private const val CLAIM_EMAIL_VERIFIED = "email_verified"
        private const val CLAIM_IS_PRIVATE_EMAIL = "is_private_email"
        // Apple은 name, picture를 기본적으로 제공하지 않음
        // 최초 인증 시에만 클라이언트에서 받아야 함
    }

    /**
     * Apple ID 토큰을 검증하고 OAuth 정보를 추출합니다.
     *
     * @param idToken Apple ID 토큰
     * @param publicKeys Apple 공개키 목록
     * @return OAuth 정보 (Provider 타입, OID, email)
     */
    override fun getOauthInfoByIdToken(
        idToken: String,
        publicKeys: OIDCPublicKeysPayload,
        platform: Platform,
    ): OauthInfoPayload {
        // Step 1: 헤더에서 kid 추출
        val kid: String = extractKidFromTokenHeader(idToken)

        // Step 2: kid로 jwks 조회
        val publicKey: OIDCPublicKeyDto = findPublicKeyByKid(publicKeys.keys, kid)

        // Step 3-6: 토큰 검증 및 Claims 추출
        // Apple은 플랫폼 구분 불필요, 동일한 clientId 사용
        val payload: OIDCDecodePayload = validateTokenAndExtractPayload(
            token = idToken,
            publicKey = publicKey,
            expectedIssuer = oauthProperties.apple.issuer,
            expectedAudience = oauthProperties.apple.clientId,
        )

        return OauthInfoPayload(
            providerType = ProviderType.APPLE,
            oid = payload.sub, // String 그대로 사용 (UUID)
            email = payload.email,
            name = payload.email?.substringBefore("@"), // Apple은 name을 제공하지 않음, email 앞부분 사용
            imageUrl = null, // Apple은 프로필 이미지를 제공하지 않음
        )
    }

    /**
     * Step 1: ID 토큰 헤더에서 kid 추출
     */
    private fun extractKidFromTokenHeader(token: String): String {
        try {
            val headerJson = String(Base64.getUrlDecoder().decode(token.split(".")[0]))
            val header = objectMapper.readValue(headerJson, Map::class.java)

            return header[HEADER_KID] as? String
                ?: throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
        } catch (e: Exception) {
            throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
        }
    }

    /**
     * Step 2: kid로 공개키 목록에서 해당 공개키 찾기
     */
    private fun findPublicKeyByKid(keys: List<OIDCPublicKeyDto>, kid: String): OIDCPublicKeyDto =
        keys.find { it.kid == kid }
            ?: throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)

    /**
     * Step 3-6: 토큰 검증 및 페이로드 추출
     * - Step 3: iss 값 검증 (requireIssuer)
     * - Step 4: aud 값 검증 (requireAudience)
     * - Step 5: exp 값 검증 (자동, ExpiredJwtException 발생)
     * - Step 6: 서명 검증 (verifyWith)
     */
    private fun validateTokenAndExtractPayload(
        token: String,
        publicKey: OIDCPublicKeyDto,
        expectedIssuer: String,
        expectedAudience: String,
    ): OIDCDecodePayload {
        val rsaPublicKey = convertToRSAPublicKey(publicKey.n, publicKey.e)
        val claims = verifyTokenSignatureAndClaims(token, rsaPublicKey, expectedIssuer, expectedAudience)

        return OIDCDecodePayload(
            iss = claims.issuer,
            aud = claims.audience.toString(),
            sub = claims.subject, // String 그대로 사용 (Apple UUID)
            email = claims[CLAIM_EMAIL, String::class.java],
            nickname = claims[CLAIM_EMAIL, String::class.java]?.substringBefore("@"), // email 앞부분
            imageUrl = null,
        )
    }

    /**
     * JWT 서명 검증 및 Claims 검증
     */
    private fun verifyTokenSignatureAndClaims(
        token: String,
        publicKey: RSAPublicKey,
        expectedIssuer: String,
        expectedAudience: String,
    ): Claims = try {
        Jwts.parser()
            .verifyWith(publicKey) // Step 6: 서명 검증
            .requireIssuer(expectedIssuer) // Step 3: iss 검증
            .requireAudience(expectedAudience) // Step 4: aud 검증
            .build()
            .parseSignedClaims(token) // Step 5: exp 자동 검증
            .payload
    } catch (e: ExpiredJwtException) {
        e.printStackTrace()
        throw BusinessException(ResultCode.EXPIRED_TOKEN_ERROR)
    } catch (e: Exception) {
        e.printStackTrace()
        throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
    }

    /**
     * JWK의 modulus(n)와 exponent(e)를 RSA 공개키로 변환
     */
    private fun convertToRSAPublicKey(modulus: String, exponent: String): RSAPublicKey {
        val keyFactory = KeyFactory.getInstance("RSA")
        val n = BigInteger(1, Base64.getUrlDecoder().decode(modulus))
        val e = BigInteger(1, Base64.getUrlDecoder().decode(exponent))
        val keySpec = RSAPublicKeySpec(n, e)
        return keyFactory.generatePublic(keySpec) as RSAPublicKey
    }
}
