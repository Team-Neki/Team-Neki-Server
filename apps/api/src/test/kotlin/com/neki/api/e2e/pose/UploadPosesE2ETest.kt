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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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

    @Test
    @DisplayName("sourceKey 와 relation 을 보내면 저장된다")
    fun sourceKeyIsSaved() {
        val media = createMedia(ownerId = admin.id!!, status = MediaStatus.INITIATED)

        upload(
            """{"uploads":[{"mediaId":${media.id},"headCount":"TWO","memo":null,""" +
                """"relation":"FRIEND","sourceKey":"friend_001_F1-4_F2-4"}]}""",
        )
            .then()
            .statusCode(HttpStatus.OK.value())

        val pose = poseRepository.findAll().single()
        pose.sourceKey shouldBe "friend_001_F1-4_F2-4"
        pose.relation shouldBe PoseRelation.FRIEND
        mediaRepository.findById(media.id!!).get().status shouldBe MediaStatus.UPLOADED
    }

    @Test
    @DisplayName("요청 안에서 sourceKey 가 중복되면 400 D-01, 포즈 0건")
    fun duplicateSourceKeyInRequest() {
        val media1 = createMedia(ownerId = admin.id!!, status = MediaStatus.INITIATED)
        val media2 = createMedia(ownerId = admin.id!!, status = MediaStatus.INITIATED)

        upload(
            """{"uploads":[
                {"mediaId":${media1.id},"headCount":"TWO","memo":null,"sourceKey":"k1"},
                {"mediaId":${media2.id},"headCount":"TWO","memo":null,"sourceKey":"k1"}
            ]}""",
        )
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))

        poseRepository.count() shouldBe 0
    }

    @ParameterizedTest(name = "sourceKey=\"{0}\"")
    @ValueSource(strings = ["a b", "", "한글", "x/y"])
    @DisplayName("sourceKey 형식 오류는 400 D-01, 포즈 0건")
    fun invalidSourceKeyFormat(sourceKey: String) {
        val media = createMedia(ownerId = admin.id!!, status = MediaStatus.INITIATED)

        upload("""{"uploads":[{"mediaId":${media.id},"headCount":"TWO","memo":null,"sourceKey":"$sourceKey"}]}""")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))

        poseRepository.count() shouldBe 0
    }

    @Test
    @DisplayName("sourceKey 101자는 400 D-01, 100자는 200")
    fun sourceKeyLength() {
        val media1 = createMedia(ownerId = admin.id!!, status = MediaStatus.INITIATED)
        val media2 = createMedia(ownerId = admin.id!!, status = MediaStatus.INITIATED)

        upload(
            """{"uploads":[{"mediaId":${media1.id},"headCount":"TWO","memo":null,"sourceKey":"${"a".repeat(101)}"}]}""",
        )
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
        upload(
            """{"uploads":[{"mediaId":${media2.id},"headCount":"TWO","memo":null,"sourceKey":"${"a".repeat(100)}"}]}""",
        )
            .then()
            .statusCode(HttpStatus.OK.value())

        poseRepository.findAll().single().sourceKey shouldBe "a".repeat(100)
    }

    @Test
    @DisplayName("이미 등록된 sourceKey 가 섞이면 409 D-18, 아무것도 만들지 않고 media 는 INITIATED. 같은 media 로 새 키만 다시 보내면 200")
    fun conflictIsAllOrNothingAndRetryable() {
        // given
        val existingMedia = createMedia(ownerId = admin.id!!, status = MediaStatus.UPLOADED)
        val existing = createPose(
            userId = admin.id!!,
            mediaId = existingMedia.id!!,
            headCount = HeadCount.TWO,
            relation = PoseRelation.FRIEND,
            sourceKey = "friend_001",
        )
        val media1 = createMedia(ownerId = admin.id!!, status = MediaStatus.INITIATED)
        val media2 = createMedia(ownerId = admin.id!!, status = MediaStatus.INITIATED)

        // when
        upload(
            """{"uploads":[
                {"mediaId":${media1.id},"headCount":"TWO","memo":null,"relation":"FRIEND","sourceKey":"friend_001"},
                {"mediaId":${media2.id},"headCount":"TWO","memo":null,"relation":"FRIEND","sourceKey":"friend_002"}
            ]}""",
        )
            .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("resultCode", equalTo(ResultCode.CONFLICT_POSE_SOURCE_KEY.code))

        // then
        poseRepository.findAll().map { it.id } shouldBe listOf(existing.id)
        mediaRepository.findById(media1.id!!).get().status shouldBe MediaStatus.INITIATED
        mediaRepository.findById(media2.id!!).get().status shouldBe MediaStatus.INITIATED

        // 같은 media2 로 새 키만 다시 보내면 성공한다
        upload(
            """{"uploads":[{"mediaId":${media2.id},"headCount":"TWO","memo":null,"relation":"FRIEND","sourceKey":"friend_002"}]}""",
        )
            .then()
            .statusCode(HttpStatus.OK.value())

        poseRepository.findAll().mapNotNull { it.sourceKey }.toSet() shouldBe setOf("friend_001", "friend_002")
        mediaRepository.findById(media2.id!!).get().status shouldBe MediaStatus.UPLOADED
    }

    @Test
    @DisplayName("sourceKey 는 대소문자를 구분한다 - friend_001 이 있어도 Friend_001 은 200")
    fun sourceKeyIsCaseSensitive() {
        val existingMedia = createMedia(ownerId = admin.id!!, status = MediaStatus.UPLOADED)
        createPose(
            userId = admin.id!!,
            mediaId = existingMedia.id!!,
            headCount = HeadCount.TWO,
            sourceKey = "friend_001",
        )
        val media = createMedia(ownerId = admin.id!!, status = MediaStatus.INITIATED)

        upload("""{"uploads":[{"mediaId":${media.id},"headCount":"TWO","memo":null,"sourceKey":"Friend_001"}]}""")
            .then()
            .statusCode(HttpStatus.OK.value())

        poseRepository.findAll().mapNotNull { it.sourceKey }.toSet() shouldBe setOf("friend_001", "Friend_001")
    }
}
