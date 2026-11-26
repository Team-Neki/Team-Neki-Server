package com.neki.api.share.api.dto

import com.neki.api.share.application.dto.ShareResult
import com.neki.api.share.config.ShareProperties
import com.neki.domain.share.models.SharedImageContent
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.time.Duration
import java.time.LocalDateTime

class ShareConverterTest {

    private val converter = ShareConverter.ResponseConverter(ShareProperties(webUrl = "https://share.example.com"))
    private val expiresAt: LocalDateTime = LocalDateTime.of(2026, 12, 3, 12, 0, 0)

    @Test
    @DisplayName("발급 응답 url 은 공유 웹페이지 주소 webUrl + /s/ + token")
    fun `발급 응답 url`() {
        // When
        val response: ShareLinkResponse.IssueShareLink =
            converter.toIssueShareLinkResponse(ShareResult.IssueShareLink(token = "tok", expiresAt = expiresAt))

        // Then
        response shouldBe ShareLinkResponse.IssueShareLink(
            token = "tok",
            url = "https://share.example.com/s/tok",
            expiresAt = expiresAt,
        )
    }

    @Test
    @DisplayName("Binary 는 200 과 공통 헤더, 보안 헤더를 붙인다")
    fun `Binary 응답 헤더`() {
        // Given
        val result = ShareResult.ViewSharedImage(
            content = SharedImageContent.Binary(byteArrayOf(1, 2, 3), "image/jpeg"),
            maxAge = Duration.ofSeconds(600),
            expiresAt = expiresAt,
        )

        // When
        val response: ResponseEntity<ByteArray> = converter.toSharedImageResponse(result)

        // Then
        val headers: HttpHeaders = response.headers
        response.statusCode shouldBe HttpStatus.OK
        response.body.contentEquals(byteArrayOf(1, 2, 3)) shouldBe true
        headers.contentType shouldBe MediaType.IMAGE_JPEG
        headers.contentLength shouldBe 3L
        headers.cacheControl shouldBe "max-age=600, private"
        headers.getFirst("X-Share-Expires-At") shouldBe "2026-12-03T12:00:00+09:00"
        headers.getFirst("Referrer-Policy") shouldBe "no-referrer"
        headers.getFirst("X-Content-Type-Options") shouldBe "nosniff"
        headers.getFirst("Content-Security-Policy") shouldBe "default-src 'none'; style-src 'unsafe-inline'; sandbox"
        headers.location shouldBe null
    }

    @Test
    @DisplayName("Redirect 는 302 와 Location, Binary 와 같은 공통 헤더를 붙인다")
    fun `Redirect 응답 헤더`() {
        // Given
        val result = ShareResult.ViewSharedImage(
            content = SharedImageContent.Redirect("https://cdn.example.com/signed?x=1"),
            maxAge = Duration.ofSeconds(600),
            expiresAt = expiresAt,
        )

        // When
        val response: ResponseEntity<ByteArray> = converter.toSharedImageResponse(result)

        // Then
        val headers: HttpHeaders = response.headers
        response.statusCode shouldBe HttpStatus.FOUND
        response.body shouldBe null
        headers.location.toString() shouldBe "https://cdn.example.com/signed?x=1"
        headers.cacheControl shouldBe "max-age=600, private"
        headers.getFirst("X-Share-Expires-At") shouldBe "2026-12-03T12:00:00+09:00"
        headers.getFirst("Referrer-Policy") shouldBe "no-referrer"
    }

    @Test
    @DisplayName("X-Share-Expires-At 은 소수 초가 있어도 초 단위까지만 출력한다")
    fun `만료 헤더는 소수 초를 출력하지 않는다`() {
        // Given
        val result = ShareResult.ViewSharedImage(
            content = SharedImageContent.Binary(byteArrayOf(1), "image/png"),
            maxAge = Duration.ofSeconds(60),
            expiresAt = expiresAt.withNano(123_456_000),
        )

        // When
        val response: ResponseEntity<ByteArray> = converter.toSharedImageResponse(result)

        // Then
        response.headers.getFirst("X-Share-Expires-At") shouldBe "2026-12-03T12:00:00+09:00"
    }

    @Test
    @DisplayName("webUrl 끝에 슬래시가 있어도 url 에 슬래시가 겹치지 않는다")
    fun `webUrl 끝 슬래시 정규화`() {
        // Given
        val trailingSlash = ShareConverter.ResponseConverter(ShareProperties(webUrl = "https://share.example.com/"))

        // When
        val response: ShareLinkResponse.IssueShareLink =
            trailingSlash.toIssueShareLinkResponse(ShareResult.IssueShareLink(token = "tok", expiresAt = expiresAt))

        // Then
        response.url shouldBe "https://share.example.com/s/tok"
    }

    @Test
    @DisplayName("webUrlQuery 가 있으면 url 에 ?{query} 를 붙인다 (staging/local: env=dev)")
    fun `webUrlQuery 가 있으면 쿼리를 붙인다`() {
        // Given
        val devConverter = ShareConverter.ResponseConverter(
            ShareProperties(webUrl = "https://share.example.com", webUrlQuery = "env=dev"),
        )

        // When
        val response: ShareLinkResponse.IssueShareLink =
            devConverter.toIssueShareLinkResponse(ShareResult.IssueShareLink(token = "tok", expiresAt = expiresAt))

        // Then
        response.url shouldBe "https://share.example.com/s/tok?env=dev"
    }

    @Test
    @DisplayName("webUrlQuery 앞에 ? 를 적어도 한 번만 붙는다")
    fun `webUrlQuery 앞 물음표 정규화`() {
        // Given
        val devConverter = ShareConverter.ResponseConverter(
            ShareProperties(webUrl = "https://share.example.com/", webUrlQuery = "?env=dev"),
        )

        // When
        val response: ShareLinkResponse.IssueShareLink =
            devConverter.toIssueShareLinkResponse(ShareResult.IssueShareLink(token = "tok", expiresAt = expiresAt))

        // Then
        response.url shouldBe "https://share.example.com/s/tok?env=dev"
    }

    @Test
    @DisplayName("webUrlQuery 가 비어 있으면 쿼리를 붙이지 않는다 (prod)")
    fun `webUrlQuery 가 비면 쿼리가 없다`() {
        // Given
        val prodConverter = ShareConverter.ResponseConverter(
            ShareProperties(webUrl = "https://share.example.com", webUrlQuery = ""),
        )

        // When
        val response: ShareLinkResponse.IssueShareLink =
            prodConverter.toIssueShareLinkResponse(ShareResult.IssueShareLink(token = "tok", expiresAt = expiresAt))

        // Then
        response.url shouldBe "https://share.example.com/s/tok"
    }
}
