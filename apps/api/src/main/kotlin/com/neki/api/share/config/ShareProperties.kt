package com.neki.api.share.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * fileName       : ShareProperties
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 사진 공유 링크 설정
 * - expiration: 공유 링크 만료 기간
 * - cacheMaxAge: 공유 응답 Cache-Control max-age 상한. 실제 값은 min(남은 시간, cacheMaxAge)
 * - webUrl: 공유 웹페이지 origin. 발급 응답 url 은 {webUrl}/s/{token}[?{webUrlQuery}] (카톡 전송용). 이미지 주소는 /share/{token}
 *   기본값을 두지 않고 @NotBlank 로 검증해 설정이 빠지면 기동 시 바로 드러나게 한다. 끝 슬래시는 shareUrl() 에서 정규화한다.
 * - webUrlQuery: 공유 웹페이지 url 에 붙일 쿼리. 웹이 하나라 staging/local 은 env=dev 로 개발 백엔드를 고르게 한다.
 *   비어 있으면(prod, 공통) 쿼리를 붙이지 않는다.
 *   인코딩 없이 그대로 붙는다. 이미 인코딩된 k=v[&k=v] 형식만 쓰고 공백과 # 은 넣지 않는다.
 *
 * UseCase(application)가 읽으므로 domain infra 나 com.neki.config 에 두지 않는다 (ArchitectureRulesTest).
 */
@Validated
@ConfigurationProperties(prefix = "app.share")
data class ShareProperties(
    val expiration: Duration = Duration.ofDays(7),
    val cacheMaxAge: Duration = Duration.ofHours(1),
    @field:NotBlank
    val webUrl: String,
    val webUrlQuery: String = "",
) {
    /**
     * 공유 웹페이지 주소. webUrl 끝 슬래시 유무와 관계없이 {origin}/s/{token} 으로 만들고,
     * webUrlQuery 가 있으면 ?{query} 를 붙인다 (앞에 ? 를 적어도 한 번만 붙는다).
     */
    fun shareUrl(token: String): String {
        val base = "${webUrl.trimEnd('/')}$SHARE_WEB_PATH$token"
        val query: String = webUrlQuery.trim().removePrefix("?")
        return if (query.isEmpty()) base else "$base?$query"
    }

    companion object {
        private const val SHARE_WEB_PATH = "/s/"
    }
}
