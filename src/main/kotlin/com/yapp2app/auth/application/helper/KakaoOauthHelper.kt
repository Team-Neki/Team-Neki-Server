package com.yapp2app.auth.application.helper

import com.fasterxml.jackson.databind.ObjectMapper
import com.yapp2app.auth.infra.response.OIDCDecodePayloadResponse
import com.yapp2app.auth.infra.response.OIDCPublicKeyDto
import com.yapp2app.auth.infra.response.OIDCPublicKeysResponse
import com.yapp2app.auth.infra.response.OauthInfoResponse
import com.yapp2app.auth.infra.security.properties.OauthProperties
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.domain.enums.ProviderType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

/**
 * fileName       : KakaoOauthHelper
 * author         : darren
 * date           : 2025. 12. 28. 21:48
 * description    : 카카오 OIDC 토큰 검증 헬퍼
 *
 * 참고: https://devnm.tistory.com/35
 * 카카오 OIDC ID 토큰 검증 절차:
 * 1. ID 토큰의 영역 구분자인 온점(.)을 기준으로 헤더, 페이로드, 서명을 분리 페이로드를 Base64 방식으로 디코딩
 * 2. idToken의 kid jwks 검증
 * 3. 페이로드의 iss 값이 https://kauth.kakao.com와 일치하는지 확인
 * 4. 페이로드의 aud 값이 서비스 앱 키와 일치하는지 확인 APP일때는 APP_ID로 체크해야함 application-local만 REST_ID(clientId)
 * 5. 페이로드의 exp 값이 현재 UNIX 타임스탬프(Timestamp)보다 큰 값인지 확인(ID 토큰이 만료되지 않았는지 확인)
 * 6. 페이로드의 nonce 값이 카카오 로그인 요청 시 전달한 값과 일치하는지 확인
 * 7. 서명 검증
 */
@Component
class KakaoOauthHelper(private val oauthProperties: OauthProperties, private val objectMapper: ObjectMapper) {

    companion object {
        private const val HEADER_KID = "kid"
        private const val CLAIM_EMAIL = "email"
        private const val CLAIM_NICKNAME = "nickname"
    }

    /**
     * 카카오 ID 토큰을 검증하고 OAuth 정보를 추출합니다.
     *
     * @param idToken 카카오 ID 토큰
     * @param publicKeys 카카오 공개키 목록
     * @return OAuth 정보 (Provider 타입, OID)
     */
    fun getOauthInfoByIdToken(idToken: String, publicKeys: OIDCPublicKeysResponse): OauthInfoResponse {
        // Step 1: 헤더에서 kid 추출 (토큰 분리 및 Base64 디코딩)
        val kid = extractKidFromTokenHeader(idToken)

        // Step 2: kid로 jwks 조회
        val publicKey = findPublicKeyByKid(publicKeys.keys, kid)

        // Step 3-7: 토큰 검증 및 Claims 추출 (iss, aud, exp, 서명 검증)
        val payload = validateTokenAndExtractPayload(
            token = idToken,
            publicKey = publicKey,
            expectedIssuer = oauthProperties.kakao.issuer,
            expectedAudience = oauthProperties.kakao.clientId,
        )

        return OauthInfoResponse(
            providerType = ProviderType.KAKAO,
            oid = payload.sub,
            email = payload.email,
            name = payload.nickname,
        )
    }

    /**
     * Step 1: ID 토큰 헤더에서 kid 추출
     * - 토큰을 온점(.)으로 분리
     * - 헤더 부분을 Base64 디코딩
     * - JSON 파싱하여 kid 추출
     */
    private fun extractKidFromTokenHeader(token: String): String {
        try {
            val headerJson = String(Base64.getUrlDecoder().decode(token.split(".")[0]))
            val header = objectMapper.readValue(headerJson, Map::class.java)

            return header[HEADER_KID] as? String
                ?: throw BusinessException(
                    ResultCode.INVALID_TOKEN_ERROR,
                )
        } catch (e: Exception) {
            throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
        }
    }

    /**
     * Step 2: kid로 공개키 목록에서 해당 공개키 찾기
     */
    private fun findPublicKeyByKid(keys: List<OIDCPublicKeyDto>, kid: String): OIDCPublicKeyDto =
        keys.find { it.kid == kid }
            ?: throw BusinessException(
                ResultCode.INVALID_TOKEN_ERROR,
            )

    /**
     * Step 3-7: 토큰 검증 및 페이로드 추출
     * - Step 3: iss 값 검증 (requireIssuer)
     * - Step 4: aud 값 검증 (requireAudience)
     * - Step 5: exp 값 검증 (자동, ExpiredJwtException 발생)
     * - Step 6: nonce 검증 (선택사항, 현재 미구현)
     * - Step 7: 서명 검증 (verifyWith)
     */
    private fun validateTokenAndExtractPayload(
        token: String,
        publicKey: OIDCPublicKeyDto,
        expectedIssuer: String,
        expectedAudience: String,
    ): OIDCDecodePayloadResponse {
        val rsaPublicKey = convertToRSAPublicKey(publicKey.n, publicKey.e)
        val claims = verifyTokenSignatureAndClaims(token, rsaPublicKey, expectedIssuer, expectedAudience)

        return OIDCDecodePayloadResponse(
            iss = claims.issuer,
            aud = claims.audience.toString(),
            sub = claims.subject.toLong(),
            email = claims[CLAIM_EMAIL, String::class.java],
            nickname = claims[CLAIM_NICKNAME, String::class.java],
        )
    }

    /**
     * JWT 서명 검증 및 Claims 검증
     * - 서명 검증 (RSA 공개키 사용)
     * - iss, aud, exp 자동 검증
     */
    private fun verifyTokenSignatureAndClaims(
        token: String,
        publicKey: RSAPublicKey,
        expectedIssuer: String,
        expectedAudience: String,
    ): Claims = try {
        Jwts.parser()
            .verifyWith(publicKey) // Step 7: 서명 검증
            .requireIssuer(expectedIssuer) // Step 3: iss 검증
            .requireAudience(expectedAudience) // Step 4: aud 검증
            .build()
            .parseSignedClaims(token) // Step 5: exp 자동 검증
            .payload
    } catch (e: ExpiredJwtException) {
        throw BusinessException(ResultCode.EXPIRED_TOKEN_ERROR)
    } catch (e: Exception) {
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
