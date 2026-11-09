package com.neki.api.e2e.pose

import com.neki.core.code.ResultCode
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import io.kotest.matchers.shouldBe
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : UploadPosesE2ETest
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : POST /api/poses/admin/upload (application/json) E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadPosesE2ETest : PoseE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var admin: User

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (user, token) = createTestUserAndToken()
        admin = user
        accessToken = token
    }

    private fun upload(body: String, token: String = accessToken) = RestAssured.given()
        .header("Authorization", "Bearer $token")
        .contentType(ContentType.JSON)
        .body(body)
        .`when`()
        .post("/api/poses/admin/upload")

    @Test
    @DisplayName("relation 을 담아 올리면 저장된다")
    fun relationIsSaved() {
        // given
        val media = createMedia(ownerId = admin.id!!, status = MediaStatus.UPLOADED)

        // when
        upload("""{"uploads":[{"mediaId":${media.id},"headCount":"TWO","memo":null,"relation":"COUPLE"}]}""")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // then
        val pose = poseRepository.findAll().single()
        pose.relation shouldBe PoseRelation.COUPLE
        pose.headCount shouldBe HeadCount.TWO
        pose.sourceKey shouldBe null
    }

    @Test
    @DisplayName("relation 이 없는 기존 형식도 성공한다")
    fun legacyFormat() {
        val media = createMedia(ownerId = admin.id!!, status = MediaStatus.UPLOADED)

        upload("""{"uploads":[{"mediaId":${media.id},"headCount":"ONE","memo":"m"}]}""")
            .then()
            .statusCode(HttpStatus.OK.value())

        poseRepository.findAll().single().relation shouldBe null
    }

    @Test
    @DisplayName("1인 포즈에 relation 을 붙이면 요청 전체가 400 D-01, 아무것도 저장하지 않는다")
    fun relationOnOneIsRejected() {
        val media1 = createMedia(ownerId = admin.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = admin.id!!, status = MediaStatus.UPLOADED)

        upload(
            """{"uploads":[
                {"mediaId":${media1.id},"headCount":"TWO","memo":null,"relation":"FRIEND"},
                {"mediaId":${media2.id},"headCount":"ONE","memo":null,"relation":"FRIEND"}
            ]}""",
        )
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))

        poseRepository.count() shouldBe 0
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403")
    fun forbiddenForUser() {
        val (user, userToken) = createTestUserAndToken(roles = RoleType.USER.role)
        val media = createMedia(ownerId = user.id!!, status = MediaStatus.UPLOADED)

        upload("""{"uploads":[{"mediaId":${media.id},"headCount":"TWO","memo":null}]}""", userToken)
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())

        poseRepository.count() shouldBe 0
    }
}
