package com.neki.user.infra.security.oauth.helper

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.application.port.dto.AuthContract
import com.neki.user.domain.enums.Platform
import com.neki.user.domain.enums.ProviderType
import com.neki.user.infra.security.config.OauthProperties
import com.neki.user.infra.security.oauth.helper.OIDCDecodePayload
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import kotlin.collections.get

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
class KakaoOauthHelper(private val oauthProperties: OauthProperties, private val objectMapper: ObjectMapper) :
    OauthHelper {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val HEADER_KID = "kid"
        private const val CLAIM_EMAIL = "email"
        private const val CLAIM_NICKNAME = "nickname"
        private const val CLAIM_PROFILEIMAGE = "picture"
    }

    /**
     * 카카오 ID 토큰을 검증하고 OAuth 정보를 추출합니다.
     *
     * @param idToken 카카오 ID 토큰
     * @param publicKeys 카카오 공개키 목록
     * @return OAuth 정보 (Provider 타입, OID)
     */
    override fun getOauthInfoByIdToken(
        idToken: String,
        publicKeys: AuthContract.OIDCPublicKeysPayload,
        platform: Platform,
    ): AuthContract.OauthInfoPayload {
        // Step 1: 헤더에서 kid 추출 (토큰 분리 및 Base64 디코딩)
        val kid: String = extractKidFromTokenHeader(idToken)

        // Step 2: kid로 jwks 조회
        val publicKey: AuthContract.OIDCPublicKey = findPublicKeyByKid(publicKeys.keys, kid)

        // Step 3-7: 토큰 검증 및 Claims 추출 (iss, aud, exp, 서명 검증)
        // 플랫폼별 clientId 선택
        val expectedAudience = when (platform) {
            Platform.IOS -> oauthProperties.kakao.iosClientId
            Platform.ANDROID -> oauthProperties.kakao.androidClientId
        }

        val payload: OIDCDecodePayload = validateTokenAndExtractPayload(
            token = idToken,
            publicKey = publicKey,
            expectedIssuer = oauthProperties.kakao.issuer,
            expectedAudience = expectedAudience,
        )

        return AuthContract.OauthInfoPayload(
            providerType = ProviderType.KAKAO,
            oid = payload.sub,
            email = payload.email,
            name = payload.nickname,
            imageUrl = payload.imageUrl,
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
            val header: Map<*, *> = objectMapper.readValue(headerJson, Map::class.java)

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
    private fun findPublicKeyByKid(keys: List<AuthContract.OIDCPublicKey>, kid: String): AuthContract.OIDCPublicKey =
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
        publicKey: AuthContract.OIDCPublicKey,
        expectedIssuer: String,
        expectedAudience: String,
    ): OIDCDecodePayload {
        val rsaPublicKey: RSAPublicKey = convertToRSAPublicKey(publicKey.n, publicKey.e)
        val claims: Claims = verifyTokenSignatureAndClaims(token, rsaPublicKey, expectedIssuer, expectedAudience)

        return OIDCDecodePayload(
            iss = claims.issuer,
            aud = claims.audience.toString(),
            sub = claims.subject,
            email = claims[CLAIM_EMAIL, String::class.java],
            nickname = claims[CLAIM_NICKNAME, String::class.java],
            imageUrl = claims[CLAIM_PROFILEIMAGE, String::class.java],
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
        e.printStackTrace()
        log.error("Token verification failed: {}", e) // TODO 캐싱이후로 oidc검증 실패시 어떤 예외 찍히는지 모니터링용
        throw BusinessException(ResultCode.EXPIRED_TOKEN_ERROR)
    } catch (e: Exception) {
        e.printStackTrace()
        log.error("Token verification failed: {}", e) // TODO 캐싱이후로 oidc검증 실패시 어떤 예외 찍히는지 모니터링용
        throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
    }

    /**
     * JWK의 modulus(n)와 exponent(e)를 RSA 공개키로 변환
     */
    private fun convertToRSAPublicKey(modulus: String, exponent: String): RSAPublicKey {
        try {
            val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
            val n = BigInteger(1, Base64.getUrlDecoder().decode(modulus))
            val e = BigInteger(1, Base64.getUrlDecoder().decode(exponent))
            val keySpec = RSAPublicKeySpec(n, e)
            return keyFactory.generatePublic(keySpec) as RSAPublicKey
        } catch (e: Exception) {
            e.printStackTrace()
            log.error("Token verification failed: {}", e) // TODO 캐싱이후로 oidc검증 실패시 어떤 예외 찍히는지 모니터링용
            throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
        }
    }
}
