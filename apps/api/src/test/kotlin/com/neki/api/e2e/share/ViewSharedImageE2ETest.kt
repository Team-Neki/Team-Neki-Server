package com.neki.api.e2e.share

import com.neki.core.code.ResultCode
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.photo.models.PhotoImage
import com.neki.domain.share.models.ShareLink
import com.neki.domain.user.models.User
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeBetween
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.restassured.RestAssured
import io.restassured.http.Header
import io.restassured.response.ExtractableResponse
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * fileName       : ViewSharedImageE2ETest
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : GET /share/{token} E2E 테스트 (200 헤더, 404/410, HEAD)
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ViewSharedImageE2ETest : ShareE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        testUser = createTestUserAndToken().first
    }

    private fun view(token: String, accept: String? = null): ExtractableResponse<Response> {
        val spec: RequestSpecification = RestAssured.given()
        if (accept != null) spec.header("Accept", accept)
        return spec.`when`().get("/share/$token").then().extract()
    }

    private fun head(token: String): ExtractableResponse<Response> =
        RestAssured.given().`when`().head("/share/$token").then().extract()

    private fun maxAgeOf(response: ExtractableResponse<Response>): Int =
        Regex("max-age=(\\d+)").find(response.header("Cache-Control"))!!.groupValues[1].toInt()

    private fun ExtractableResponse<Response>.headerValues(name: String): List<String> = headers().getValues(name)

    @Nested
    @DisplayName("200")
    inner class SuccessTests {

        @Test
        @DisplayName("유효한 토큰이면 이미지 바이트와 공유 응답 헤더를 돌려준다")
        fun givenValidLink_whenView_thenReturnsImageWithHeaders() {
            // given
            val (photo, media) = createSharablePhoto(testUser.id!!)
            val link: ShareLink = saveShareLink(testUser.id!!, photo.id!!)

            // when
            val response: ExtractableResponse<Response> = view(link.token)

            // then
            response.statusCode() shouldBe HttpStatus.OK.value()
            val body: ByteArray = response.asByteArray()
            body.size shouldBeGreaterThan 0
            body.contentEquals(imageBytes(media.storageKey)) shouldBe true
            response.contentType() shouldBe "image/jpeg"
            response.header("Content-Length") shouldBe body.size.toString()

            response.header("Cache-Control") shouldContain "private"
            maxAgeOf(response).shouldBeBetween(1, 3600)

            val expiresAtHeader: String = response.header("X-Share-Expires-At")
            expiresAtHeader shouldMatch Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00$")
            val saved: ShareLink = shareLinkRepository.findByToken(link.token)!!
            expiresAtHeader shouldBe expectedExpiresAtHeader(saved.expiresAt)

            response.header("Referrer-Policy") shouldBe "no-referrer"
            response.header("Content-Security-Policy") shouldBe "default-src 'none'; style-src 'unsafe-inline'; sandbox"
            response.headerValues("X-Content-Type-Options") shouldContainExactly listOf("nosniff")
        }

        @Test
        @DisplayName("공유 응답 Cache-Control 은 Spring Security 기본 캐시 헤더에 덮이지 않고 하나만 나간다")
        fun givenValidLink_whenView_thenSecurityCacheHeadersAreNotAdded() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val link: ShareLink = saveShareLink(testUser.id!!, photo.id!!)

            // when
            val response: ExtractableResponse<Response> = view(link.token)

            // then
            response.headerValues("Cache-Control") shouldHaveSize 1
            response.header("Cache-Control") shouldMatch Regex("^max-age=\\d+, private$")
            response.header("Cache-Control") shouldNotContain "no-store"
            response.headerValues("Pragma").shouldBeEmpty()
            response.headerValues("Expires").shouldBeEmpty()
        }

        @Test
        @DisplayName("만료 10분 전 링크는 max-age 가 600 이하다")
        fun givenLinkExpiringSoon_whenView_thenMaxAgeIsRemaining() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val link: ShareLink =
                saveShareLink(testUser.id!!, photo.id!!, expiresAt = LocalDateTime.now().plusMinutes(10))

            // when
            val response: ExtractableResponse<Response> = view(link.token)

            // then
            response.statusCode() shouldBe HttpStatus.OK.value()
            maxAgeOf(response).shouldBeBetween(1, 600)
            response.header("X-Share-Expires-At") shouldBe expectedExpiresAtHeader(link.expiresAt)
        }

        @Test
        @DisplayName("응답 헤더 어디에도 storageKey 나 /file/image 가 없고 Location 도 없다")
        fun givenValidLink_whenView_thenNoObjectKeyExposed() {
            // given
            val (photo, media) = createSharablePhoto(testUser.id!!)
            val link: ShareLink = saveShareLink(testUser.id!!, photo.id!!)

            // when
            val response: ExtractableResponse<Response> = view(link.token)

            // then
            val allHeaders: String = response.headers().asList().joinToString("\n") { "${it.name}: ${it.value}" }
            allHeaders shouldNotContain media.storageKey
            allHeaders shouldNotContain "/file/image"
            response.header("Location") shouldBe null
        }
    }

    @Nested
    @DisplayName("404")
    inner class NotFoundTests {

        @Test
        @DisplayName("형식은 맞지만 없는 토큰은 404 D-19, JSON, 만료 헤더 없음")
        fun givenUnknownToken_whenView_thenNotFound() {
            // when
            val response: ExtractableResponse<Response> = view(randomToken())

            // then
            response.statusCode() shouldBe HttpStatus.NOT_FOUND.value()
            response.contentType() shouldStartWith "application/json"
            response.path<String>("resultCode") shouldBe ResultCode.SHARE_NOT_FOUND.code
            response.header("X-Share-Expires-At") shouldBe null
            // Spring Security 기본 no-store 가 붙어 nginx/브라우저가 404 를 캐시하지 않는다
            response.header("Cache-Control") shouldContain "no-store"
        }

        @Test
        @DisplayName("형식이 틀린 토큰은 404 D-19")
        fun givenMalformedToken_whenView_thenNotFound() {
            // when
            val response: ExtractableResponse<Response> = view("abc")

            // then
            response.statusCode() shouldBe HttpStatus.NOT_FOUND.value()
            response.path<String>("resultCode") shouldBe ResultCode.SHARE_NOT_FOUND.code
        }

        @Test
        @DisplayName("soft delete 된 링크는 404 D-19 (취소 API 전까지 의도된 동작)")
        fun givenDeletedLink_whenView_thenNotFound() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val link: ShareLink =
                saveShareLink(testUser.id!!, photo.id!!, deletedAt = LocalDateTime.now().minusMinutes(1))

            // when
            val response: ExtractableResponse<Response> = view(link.token)

            // then
            response.statusCode() shouldBe HttpStatus.NOT_FOUND.value()
            response.path<String>("resultCode") shouldBe ResultCode.SHARE_NOT_FOUND.code
        }
    }

    @Nested
    @DisplayName("410")
    inner class GoneTests {

        @Test
        @DisplayName("만료된 링크는 410 D-20, JSON, 만료 헤더 없음")
        fun givenExpiredLink_whenView_thenGone() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val link: ShareLink =
                saveShareLink(testUser.id!!, photo.id!!, expiresAt = LocalDateTime.now().minusSeconds(1))

            // when
            val response: ExtractableResponse<Response> = view(link.token)

            // then
            response.statusCode() shouldBe HttpStatus.GONE.value()
            response.contentType() shouldStartWith "application/json"
            response.path<String>("resultCode") shouldBe ResultCode.SHARE_EXPIRED.code
            response.header("X-Share-Expires-At") shouldBe null
            response.header("Cache-Control") shouldContain "no-store"
        }

        @Test
        @DisplayName("원본 사진을 soft delete 하면 410 D-20")
        fun givenDeletedPhoto_whenView_thenGone() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val link: ShareLink = saveShareLink(testUser.id!!, photo.id!!)
            photo.softDelete()
            photoImageRepository.save(photo)

            // when
            val response: ExtractableResponse<Response> = view(link.token)

            // then
            response.statusCode() shouldBe HttpStatus.GONE.value()
            response.path<String>("resultCode") shouldBe ResultCode.SHARE_EXPIRED.code
        }

        @Test
        @DisplayName("media 가 DELETED 면 410 D-20")
        fun givenDeletedMedia_whenView_thenGone() {
            // given
            val media: Media = createMedia(ownerId = testUser.id!!, status = MediaStatus.DELETED)
            val photo: PhotoImage = createPhoto(testUser.id!!, media.id!!)
            val link: ShareLink = saveShareLink(testUser.id!!, photo.id!!)

            // when
            val response: ExtractableResponse<Response> = view(link.token)

            // then
            response.statusCode() shouldBe HttpStatus.GONE.value()
            response.path<String>("resultCode") shouldBe ResultCode.SHARE_EXPIRED.code
        }

        @Test
        @DisplayName("없는 토큰(404)과 만료 토큰(410)은 상태코드와 resultCode 가 다르다")
        fun givenUnknownAndExpired_whenView_thenDifferentStatus() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val expired: ShareLink =
                saveShareLink(testUser.id!!, photo.id!!, expiresAt = LocalDateTime.now().minusMinutes(1))

            // when
            val unknown: ExtractableResponse<Response> = view(randomToken())
            val gone: ExtractableResponse<Response> = view(expired.token)

            // then
            unknown.statusCode() shouldNotBe gone.statusCode()
            unknown.path<String>("resultCode") shouldNotBe gone.path<String>("resultCode")
        }
    }

    @Nested
    @DisplayName("Accept: image/*")
    inner class AcceptImageTests {

        @Test
        @DisplayName("이미지 Accept 로 요청해도 없는 토큰은 404, 만료 토큰은 410 이다 (406 아님)")
        fun givenImageAccept_whenViewInvalid_thenStatusIsKept() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val expired: ShareLink =
                saveShareLink(testUser.id!!, photo.id!!, expiresAt = LocalDateTime.now().minusMinutes(1))

            // when
            val unknown: ExtractableResponse<Response> = view(randomToken(), accept = "image/*")
            val gone: ExtractableResponse<Response> = view(expired.token, accept = "image/*")

            // then
            unknown.statusCode() shouldBe HttpStatus.NOT_FOUND.value()
            gone.statusCode() shouldBe HttpStatus.GONE.value()
        }

        @Test
        @DisplayName("이미지 Accept 로 유효한 토큰을 요청하면 200")
        fun givenImageAccept_whenViewValid_thenOk() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val link: ShareLink = saveShareLink(testUser.id!!, photo.id!!)

            // when
            val response: ExtractableResponse<Response> = view(link.token, accept = "image/*")

            // then
            response.statusCode() shouldBe HttpStatus.OK.value()
        }
    }

    @Nested
    @DisplayName("HEAD")
    inner class HeadTests {

        @Test
        @DisplayName("유효한 토큰 HEAD 는 200, body 없음, GET 과 같은 헤더")
        fun givenValidLink_whenHead_thenSameHeadersAsGet() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val link: ShareLink = saveShareLink(testUser.id!!, photo.id!!)
            val get: ExtractableResponse<Response> = view(link.token)

            // when
            val head: ExtractableResponse<Response> = head(link.token)

            // then
            head.statusCode() shouldBe HttpStatus.OK.value()
            head.asByteArray().size shouldBe 0
            head.contentType() shouldBe get.contentType()
            head.header("X-Share-Expires-At") shouldBe get.header("X-Share-Expires-At")
            head.header("Cache-Control") shouldContain "private"
            head.headerValues("Cache-Control") shouldHaveSize 1
        }

        @Test
        @DisplayName("만료 토큰 HEAD 는 410")
        fun givenExpiredLink_whenHead_thenGone() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val link: ShareLink =
                saveShareLink(testUser.id!!, photo.id!!, expiresAt = LocalDateTime.now().minusMinutes(1))

            // when
            val head: ExtractableResponse<Response> = head(link.token)

            // then
            head.statusCode() shouldBe HttpStatus.GONE.value()
        }
    }

    @Nested
    @DisplayName("Security 체인 공유")
    inner class SecurityChainTests {

        @Test
        @DisplayName("인증 헤더 없이도 200 (permitAll)")
        fun givenNoAuth_whenView_thenOk() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val link: ShareLink = saveShareLink(testUser.id!!, photo.id!!)

            // when
            val response: ExtractableResponse<Response> =
                RestAssured.given().header(
                    Header("X-Test", "no-auth"),
                ).`when`().get("/share/${link.token}").then().extract()

            // then
            response.statusCode() shouldBe HttpStatus.OK.value()
        }

        @Test
        @DisplayName("matcher 를 /share/** 로 넓혀도 /file/image 응답 Cache-Control 은 max-age=86400 하나뿐이다")
        fun givenFileImage_whenGet_thenCacheHeaderUnchanged() {
            // given
            val objectKey = "photo-booth/unchanged.jpg"
            fakeStorage.putTestData(objectKey, imageBytes(objectKey))

            // when
            val response: ExtractableResponse<Response> =
                RestAssured.given().`when`().get("/file/image/$objectKey").then().extract()

            // then
            response.statusCode() shouldBe HttpStatus.OK.value()
            response.headerValues("Cache-Control") shouldContainExactly listOf("max-age=86400")
            response.headerValues("Pragma").shouldBeEmpty()
            response.headerValues("Expires").shouldBeEmpty()
        }
    }
}
