package com.neki.api.share.api.dto

import com.neki.api.share.application.dto.ShareResult
import com.neki.api.share.config.ShareProperties
import com.neki.domain.share.dto.ShareCommand
import com.neki.domain.share.dto.ShareQuery
import com.neki.domain.share.models.SharedImageContent
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.net.URI
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * fileName       : ShareConverter
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : Share api layer converter
 */
object ShareConverter {
    @Component
    class RequestConverter {
        fun toIssueShareLinkCommand(
            userId: Long,
            request: ShareLinkRequest.IssueShareLink,
        ): ShareCommand.IssueShareLink = ShareCommand.IssueShareLink(userId = userId, photoId = request.photoId!!)

        fun toViewSharedImageQuery(token: String): ShareQuery.ViewSharedImage =
            ShareQuery.ViewSharedImage(token = token)
    }

    @Component
    class ResponseConverter(private val shareProperties: ShareProperties) {
        companion object {
            const val SHARE_EXPIRES_AT_HEADER = "X-Share-Expires-At"

            // expires_at 은 KST LocalDateTime 이다 (운영 컨테이너 TZ=Asia/Seoul). 소수 초 없이 오프셋을 붙인다.
            private val SHARE_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
            private val EXPIRES_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
            )

            // 업로드 시 확장자를 검증하지 않아 svg 같은 실행 가능한 타입이 나갈 수 있다. 브라우저로 직접 열어도
            // 스크립트가 돌지 않게 막는다. style-src 'unsafe-inline' 은 브라우저 이미지 뷰어의 inline 스타일용이다.
            private const val CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'unsafe-inline'; sandbox"
        }

        /** url 은 공유 웹페이지 주소(카톡 전송용)다. 이미지 바이너리 주소(/share/{token})는 웹페이지가 호출한다 */
        fun toIssueShareLinkResponse(result: ShareResult.IssueShareLink): ShareLinkResponse.IssueShareLink =
            ShareLinkResponse.IssueShareLink(
                token = result.token,
                url = shareProperties.shareUrl(result.token),
                expiresAt = result.expiresAt,
            )

        /**
         * 200(Binary) 과 302(Redirect) 에 같은 공통 헤더를 붙인다. 전달 방식을 바꿔도 소비자가 보는 헤더는 같다.
         * 404/410 은 ExceptionHandler 가 만들며 공통 헤더를 붙이지 않는다.
         */
        fun toSharedImageResponse(result: ShareResult.ViewSharedImage): ResponseEntity<ByteArray> {
            val common: HttpHeaders = commonHeaders(result)

            return when (val content: SharedImageContent = result.content) {
                is SharedImageContent.Binary -> ResponseEntity.ok()
                    .headers(common)
                    .contentType(MediaType.parseMediaType(content.contentType))
                    .header(HttpHeaders.CONTENT_LENGTH, content.bytes.size.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Content-Security-Policy", CONTENT_SECURITY_POLICY)
                    .body(content.bytes)

                is SharedImageContent.Redirect -> ResponseEntity.status(HttpStatus.FOUND)
                    .headers(common)
                    .location(URI(content.location))
                    .build()
            }
        }

        private fun commonHeaders(result: ShareResult.ViewSharedImage): HttpHeaders = HttpHeaders().apply {
            cacheControl = CacheControl.maxAge(result.maxAge.seconds, TimeUnit.SECONDS).cachePrivate().headerValue
            set(SHARE_EXPIRES_AT_HEADER, result.expiresAt.atZone(SHARE_ZONE).format(EXPIRES_AT_FORMATTER))
            // 추후 OG HTML / 302 Redirect 에서 토큰 URL 이 Referer 로 새지 않게 한다
            set("Referrer-Policy", "no-referrer")
        }
    }
}
