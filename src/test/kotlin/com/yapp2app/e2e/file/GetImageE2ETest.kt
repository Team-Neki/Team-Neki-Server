package com.yapp2app.e2e.file

import com.yapp2app.e2e.E2ETestBase
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.infra.storage.fake.FakeMediaStorageAdapter
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetImageE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var mediaStoragePort: MediaStoragePort

    private val fakeStorage: FakeMediaStorageAdapter
        get() = mediaStoragePort as FakeMediaStorageAdapter

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @AfterEach
    override fun tearDown() {
        fakeStorage.clearTestData()
        super.tearDown()
    }

    @Test
    @DisplayName("JPEG 이미지 조회 시 올바른 Content Type 반환")
    fun givenJpegImage_whenGetImage_thenReturnsImageWithCorrectContentType() {
        val objectKey = "test/image.jpg"
        val imageData = createTestJpegData()
        fakeStorage.putTestData(objectKey, imageData)

        RestAssured.given()
            .`when`()
            .get("/file/image/$objectKey")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.IMAGE_JPEG_VALUE)
            .header("Cache-Control", equalTo("max-age=86400"))
    }

    @Test
    @DisplayName("PNG 이미지 조회 시 올바른 Content Type 반환")
    fun givenPngImage_whenGetImage_thenReturnsImageWithCorrectContentType() {
        val objectKey = "test/image.png"
        val imageData = createTestPngData()
        fakeStorage.putTestData(objectKey, imageData)

        RestAssured.given()
            .`when`()
            .get("/file/image/$objectKey")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.IMAGE_PNG_VALUE)
            .header("Cache-Control", equalTo("max-age=86400"))
    }

    @Test
    @DisplayName("중첩된 경로의 이미지 조회")
    fun givenNestedPathImage_whenGetImage_thenReturnsImage() {
        val objectKey = "users/123/photos/2026/01/image.jpeg"
        val imageData = createTestJpegData()
        fakeStorage.putTestData(objectKey, imageData)

        RestAssured.given()
            .`when`()
            .get("/file/image/$objectKey")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.IMAGE_JPEG_VALUE)
    }

    @Test
    @DisplayName("WebP 이미지 조회 시 올바른 Content Type 반환")
    fun givenWebpImage_whenGetImage_thenReturnsImageWithCorrectContentType() {
        val objectKey = "test/image.webp"
        val imageData = "webp image data".toByteArray()
        fakeStorage.putTestData(objectKey, imageData)

        RestAssured.given()
            .`when`()
            .get("/file/image/$objectKey")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType("image/webp")
    }

    @Test
    @DisplayName("GIF 이미지 조회 시 올바른 Content Type 반환")
    fun givenGifImage_whenGetImage_thenReturnsImageWithCorrectContentType() {
        val objectKey = "test/image.gif"
        val imageData = "gif image data".toByteArray()
        fakeStorage.putTestData(objectKey, imageData)

        RestAssured.given()
            .`when`()
            .get("/file/image/$objectKey")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.IMAGE_GIF_VALUE)
    }

    @Test
    @DisplayName("존재하지 않는 이미지 조회 시 빈 응답 반환")
    fun givenNonExistentImage_whenGetImage_thenReturnsEmptyResponse() {
        val objectKey = "non-existent/image.jpg"

        val response = RestAssured.given()
            .`when`()
            .get("/file/image/$objectKey")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .asByteArray()

        assert(response.isEmpty())
    }

    @Test
    @DisplayName("바이너리 데이터가 올바르게 반환됨")
    fun givenImage_whenGetImage_thenReturnsBinaryDataCorrectly() {
        val objectKey = "test/binary-test.jpg"
        val expectedData = createTestJpegData()
        fakeStorage.putTestData(objectKey, expectedData)

        val actualData = RestAssured.given()
            .`when`()
            .get("/file/image/$objectKey")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .asByteArray()

        assert(actualData.contentEquals(expectedData))
    }

    @Test
    @DisplayName("확장자 없는 파일 조회 시 기본 Content Type 반환")
    fun givenFileWithoutExtension_whenGetImage_thenReturnsDefaultContentType() {
        val objectKey = "test/file-without-extension"
        val fileData = "some binary data".toByteArray()
        fakeStorage.putTestData(objectKey, fileData)

        RestAssured.given()
            .`when`()
            .get("/file/image/$objectKey")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType("application/octet-stream")
    }

    private fun createTestJpegData(): ByteArray = byteArrayOf(
        0xFF.toByte(),
        0xD8.toByte(),
        0xFF.toByte(),
        0xE0.toByte(),
        0x00,
        0x10,
        0x4A,
        0x46,
        0x49,
        0x46,
        0x00,
        0x01,
        0x02,
        0x03,
        0x04,
        0x05,
    )

    private fun createTestPngData(): ByteArray = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A,
        0x01,
        0x02,
        0x03,
        0x04,
        0x05,
    )
}
