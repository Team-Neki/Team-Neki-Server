package com.neki.api.e2e.pose

import com.neki.core.code.ResultCode
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.infra.storage.fake.FakeMediaStorageAdapter
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.media.models.MediaType
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.restassured.RestAssured
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : BulkUploadPosesE2ETest
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : POST /api/poses/admin/upload (multipart/form-data) PM 일괄 업로드 E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BulkUploadPosesE2ETest : PoseE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var mediaStorage: MediaStorage

    private val fakeStorage: FakeMediaStorageAdapter
        get() = mediaStorage as FakeMediaStorageAdapter

    private lateinit var accessToken: String
    private lateinit var admin: User

    // PM pose.csv 와 같은 형식 (CRLF, 빈 tag). couple 은 이미지가 없다
    private val poseCsv: String = listOf(
        "media_key,head_count,tag",
        "couple_001_F1-4_M1-5,TWO,couple",
        "friend_001_F1-4_F2-4,TWO,friend",
        "friend_002_M1-5_M2-2,TWO,friend",
        "solo_001_F2-3,ONE,",
        "ssum_001_M1-2_F1-5,TWO,ssum",
    ).joinToString("\r\n", postfix = "\r\n")

    private val imageNames: List<String> = listOf(
        "friend_001_F1-4_F2-4.png",
        "friend_002_M1-5_M2-2.png",
        "solo_001_F2-3.png",
        "ssum_001_M1-2_F1-5.png",
    )

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (user, token) = createTestUserAndToken()
        admin = user
        accessToken = token
    }

    @AfterEach
    fun clearStorage() {
        fakeStorage.clearTestData()
    }

    private fun request(token: String = accessToken): RequestSpecification = RestAssured.given()
        .header("Authorization", "Bearer $token")
        .contentType("multipart/form-data")

    private fun RequestSpecification.poseCsv(csv: String = poseCsv): RequestSpecification =
        multiPart("poseCsv", "pose.csv", csv.toByteArray(), "text/csv")

    private fun RequestSpecification.images(
        names: List<String>,
        content: (String) -> ByteArray = { pngHeaderBytes(1024, 1536) },
    ): RequestSpecification = names.fold(this) { spec, name ->
        spec.multiPart("images", name, content(name), "image/png")
    }

    private fun RequestSpecification.upload(): Response = `when`().post("/api/poses/admin/upload")

    private fun poseMedias(): List<Media> = mediaRepository.findAll().filter { it.mediaType == MediaType.POSE }

    @Test
    @DisplayName("기본 시나리오 - 4건 등록, couple 은 IMAGE_NOT_PROVIDED, 목록 필터에 바로 보인다")
    fun createsPoses() {
        // when
        request().poseCsv().images(imageNames).upload()
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.summary.totalRows", equalTo(5))
            .body("data.summary.created", equalTo(4))
            .body("data.summary.skipped", equalTo(1))
            .body("data.summary.failed", equalTo(0))
            .body("data.summary.unmatchedImages", equalTo(0))
            .body("data.skipped[0].sourceKey", equalTo("couple_001_F1-4_M1-5"))
            .body("data.skipped[0].line", equalTo(2))
            .body("data.skipped[0].reason", equalTo("IMAGE_NOT_PROVIDED"))
            .body("data.created.sourceKey", equalTo(imageNames.map { it.removeSuffix(".png") }))

        // then: DB
        val poses: Map<String?, Pose> = poseRepository.findAll().associateBy { it.sourceKey }
        poses.keys shouldBe setOf("friend_001_F1-4_F2-4", "friend_002_M1-5_M2-2", "solo_001_F2-3", "ssum_001_M1-2_F1-5")
        poses.getValue("friend_001_F1-4_F2-4").relation shouldBe PoseRelation.FRIEND
        poses.getValue("ssum_001_M1-2_F1-5").relation shouldBe PoseRelation.SSUM
        poses.getValue("solo_001_F2-3").relation shouldBe null
        poses.getValue("solo_001_F2-3").headCount shouldBe HeadCount.ONE
        poses.values.forEach { it.userId shouldBe admin.id }

        val medias: List<Media> = poseMedias()
        medias.size shouldBe 4
        medias.forEach {
            it.status shouldBe MediaStatus.UPLOADED
            it.ownerId shouldBe admin.id
            it.contentType shouldBe "image/png"
            it.width shouldBe 1024
            it.height shouldBe 1536
            it.size shouldBe pngHeaderBytes(1024, 1536).size.toLong()
            it.storageKey shouldEndWith ".png"
            fakeStorage.hasKey(it.storageKey) shouldBe true
        }
        poses.values.map { it.mediaId }.toSet() shouldBe medias.map { it.id }.toSet()

        // then: 목록 API (CSV 마지막 friend 행이 맨 위)
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("headCount", "TWO")
            .queryParam("relation", "FRIEND")
            .`when`()
            .get("/api/poses")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.items", hasSize<Int>(2))
            .body("data.items[0].poseId", equalTo(poses.getValue("friend_002_M1-5_M2-2").id!!.toInt()))
            .body("data.items[0].relation", equalTo("FRIEND"))
            .body("data.items[0].width", equalTo(1024))
    }

    @Test
    @DisplayName("같은 요청을 다시 보내면 멱등 - created 0, ALREADY_REGISTERED 4, unmatched 0, 수 변화 없음")
    fun idempotentResend() {
        request().poseCsv().images(imageNames).upload().then().statusCode(HttpStatus.OK.value())

        request().poseCsv().images(imageNames).upload()
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.summary.created", equalTo(0))
            .body("data.summary.skipped", equalTo(5))
            .body("data.summary.unmatchedImages", equalTo(0))
            .body("data.unmatchedImages", empty<Any>())
            .body("data.skipped.findAll { it.reason == 'ALREADY_REGISTERED' }", hasSize<Any>(4))
            .body("data.skipped.findAll { it.reason == 'IMAGE_NOT_PROVIDED' }", hasSize<Any>(1))

        poseRepository.count() shouldBe 4
        poseMedias().size shouldBe 4
        fakeStorage.keys().size shouldBe 4
    }

    @Test
    @DisplayName("mediaCsv 의 file_name 으로 이미지를 찾는다")
    fun mapsByMediaCsv() {
        val mediaCsv = "media_key,file_name,width,height,size\nfriend_001_F1-4_F2-4,pic-a.PNG,1,1,1\n"

        request().poseCsv()
            .multiPart("mediaCsv", "media.csv", mediaCsv.toByteArray(), "text/csv")
            .images(listOf("pic-a.PNG", "stray.png"))
            .upload()
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.summary.created", equalTo(1))
            .body("data.created[0].sourceKey", equalTo("friend_001_F1-4_F2-4"))
            .body("data.unmatchedImages", equalTo(listOf("stray.png")))
    }

    @Test
    @DisplayName("media.csv 의 두 키가 같은 파일을 가리키면 두 행 모두 IMAGE_SHARED_BY_ROWS, 포즈와 media 를 만들지 않는다")
    fun sharedImageIsRejected() {
        val mediaCsv = "media_key,file_name\nfriend_001_F1-4_F2-4,same.png\nfriend_002_M1-5_M2-2,same.png\n"

        request().poseCsv()
            .multiPart("mediaCsv", "media.csv", mediaCsv.toByteArray(), "text/csv")
            .images(listOf("same.png"))
            .upload()
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.summary.created", equalTo(0))
            .body("data.failed.reason", equalTo(listOf("IMAGE_SHARED_BY_ROWS", "IMAGE_SHARED_BY_ROWS")))
            .body("data.failed.line", equalTo(listOf(3, 4)))

        poseRepository.count() shouldBe 0
        poseMedias().size shouldBe 0
        fakeStorage.keys().size shouldBe 0
    }

    @Test
    @DisplayName("JPEG 도 받는다 - 실측 크기와 image/jpeg 로 저장한다")
    fun acceptsJpeg() {
        request().poseCsv()
            .multiPart("images", "friend_001_F1-4_F2-4.jpg", jpegHeaderBytes(640, 480), "image/jpeg")
            .upload()
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.summary.created", equalTo(1))

        val media: Media = poseMedias().single()
        media.contentType shouldBe "image/jpeg"
        media.width shouldBe 640
        media.height shouldBe 480
        media.storageKey shouldEndWith ".jpg"
    }

    @Test
    @DisplayName("이미지가 아닌 .png 는 UNSUPPORTED_IMAGE, media 를 만들지 않는다")
    fun unsupportedImage() {
        request().poseCsv().images(listOf("friend_001_F1-4_F2-4.png")) { "not an image".toByteArray() }.upload()
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.summary.created", equalTo(0))
            .body("data.failed[0].reason", equalTo("UNSUPPORTED_IMAGE"))

        poseRepository.count() shouldBe 0
        poseMedias().size shouldBe 0
        fakeStorage.keys().size shouldBe 0
    }

    @Test
    @DisplayName("스토리지 업로드 실패는 STORAGE_FAILED, media 를 만들지 않고 나머지 행은 등록한다")
    fun storageFailure() {
        var calls = 0
        fakeStorage.failPutWhen { calls++ == 0 }

        request().poseCsv().images(imageNames.take(2)).upload()
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.summary.created", equalTo(1))
            .body("data.failed[0].sourceKey", equalTo("friend_001_F1-4_F2-4"))
            .body("data.failed[0].reason", equalTo("STORAGE_FAILED"))

        poseRepository.count() shouldBe 1
        poseMedias().size shouldBe 1
    }

    @Test
    @DisplayName("1인 포즈에 tag 가 있으면 RELATION_NOT_ALLOWED")
    fun relationNotAllowed() {
        val csv = "media_key,head_count,tag\r\nsolo_couple,ONE,couple\r\n"

        request().poseCsv(csv).images(listOf("solo_couple.png")).upload()
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.failed[0].reason", equalTo("RELATION_NOT_ALLOWED"))
            .body("data.failed[0].line", equalTo(2))

        poseRepository.count() shouldBe 0
        poseMedias().size shouldBe 0
    }

    @Test
    @DisplayName("poseCsv part 가 없으면 400 D-01")
    fun missingPoseCsv() {
        request().images(imageNames).upload()
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("pose.csv 헤더가 틀리면 400 D-01, 아무것도 저장하지 않는다")
    fun invalidHeader() {
        request().poseCsv("key,count\r\nfriend_001_F1-4_F2-4,TWO\r\n").images(imageNames).upload()
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))

        poseRepository.count() shouldBe 0
        fakeStorage.keys().size shouldBe 0
    }

    @Test
    @DisplayName("빈 pose.csv 는 400 D-01")
    fun emptyPoseCsv() {
        request().poseCsv("").upload()
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403, 저장하지 않는다")
    fun forbiddenForUser() {
        val (_, userToken) = createTestUserAndToken(roles = RoleType.USER.role)

        request(userToken).poseCsv().images(imageNames).upload()
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())

        poseRepository.count() shouldBe 0
        fakeStorage.keys().size shouldBe 0
    }
}
