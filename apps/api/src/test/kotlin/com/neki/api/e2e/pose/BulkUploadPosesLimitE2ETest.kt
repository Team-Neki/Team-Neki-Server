package com.neki.api.e2e.pose

import com.neki.core.code.ResultCode
import io.kotest.matchers.shouldBe
import io.restassured.RestAssured
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : BulkUploadPosesLimitE2ETest
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : 일괄 업로드 multipart 한도 초과 응답(413 D-17/D-18) E2E 테스트.
 *                  운영 한도(10MB/128MB/100 part)를 그대로 쓰면 테스트가 무거워 작은 한도로 같은 경로를 검증한다.
 */
@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.servlet.multipart.max-file-size=64KB",
        "spring.servlet.multipart.max-request-size=256KB",
        "server.tomcat.max-part-count=10",
    ],
)
class BulkUploadPosesLimitE2ETest : PoseE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String

    private val poseCsv: ByteArray = "media_key,head_count,tag\r\nfriend_001,TWO,friend\r\n".toByteArray()

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
        accessToken = createTestUserAndToken().second
    }

    private fun request(): RequestSpecification = RestAssured.given()
        .header("Authorization", "Bearer $accessToken")
        .contentType("multipart/form-data")
        .multiPart("poseCsv", "pose.csv", poseCsv, "text/csv")

    private fun RequestSpecification.images(count: Int, bytesEach: Int): RequestSpecification =
        (1..count).fold(this) { spec, i ->
            spec.multiPart("images", "x_$i.png", pngHeaderBytes(10, 10, padding = bytesEach - 33), "image/png")
        }

    private fun assertNothingSaved() {
        poseRepository.count() shouldBe 0
        mediaRepository.count() shouldBe 0
    }

    @Test
    @DisplayName("파일 하나가 max-file-size 를 넘으면 413 D-17")
    fun fileTooLarge() {
        request().images(count = 1, bytesEach = 100 * 1024)
            .`when`().post("/api/poses/admin/upload")
            .then()
            .statusCode(HttpStatus.PAYLOAD_TOO_LARGE.value())
            .body("resultCode", equalTo(ResultCode.UPLOAD_PAYLOAD_TOO_LARGE.code))

        assertNothingSaved()
    }

    @Test
    @DisplayName("요청 전체가 max-request-size 를 넘으면 413 D-17")
    fun requestTooLarge() {
        request().images(count = 5, bytesEach = 60 * 1024)
            .`when`().post("/api/poses/admin/upload")
            .then()
            .statusCode(HttpStatus.PAYLOAD_TOO_LARGE.value())
            .body("resultCode", equalTo(ResultCode.UPLOAD_PAYLOAD_TOO_LARGE.code))

        assertNothingSaved()
    }

    @Test
    @DisplayName("part 수가 max-part-count 를 넘으면 413 D-18")
    fun tooManyParts() {
        request().images(count = 10, bytesEach = 1024)
            .`when`().post("/api/poses/admin/upload")
            .then()
            .statusCode(HttpStatus.PAYLOAD_TOO_LARGE.value())
            .body("resultCode", equalTo(ResultCode.UPLOAD_TOO_MANY_PARTS.code))

        assertNothingSaved()
    }

    @Test
    @DisplayName("한도 안이면 정상 처리된다 (part 10개)")
    fun withinLimits() {
        request().images(count = 9, bytesEach = 1024)
            .`when`().post("/api/poses/admin/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.summary.totalRows", equalTo(1))
    }
}
