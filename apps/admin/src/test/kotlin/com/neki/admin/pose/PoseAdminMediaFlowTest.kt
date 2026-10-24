package com.neki.admin.pose

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.admin.auth.AdminTestSession
import com.neki.admin.common.media.JpaAdminMediaRepository
import com.neki.admin.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.core.code.ResultCode
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import com.neki.domain.user.models.RoleType
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import com.neki.domain.media.models.MediaType as DomainMediaType

/**
 * fileName       : PoseAdminMediaFlowTest
 * description    : 포즈 이미지(mediaId) 등록·교체 flow와 예외 처리 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PoseAdminMediaFlowTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var poseRepository: JpaPoseRepository

    @Autowired
    lateinit var mediaRepository: JpaAdminMediaRepository

    @Autowired
    lateinit var userRepository: JpaUserRepository

    @Autowired
    lateinit var sessions: JdbcIndexedSessionRepository

    private lateinit var adminCookie: Cookie
    private lateinit var csrf: String
    private lateinit var uploaderCookie: Cookie
    private lateinit var uploaderCsrf: String
    private var uploaderId: Long = 0

    @BeforeEach
    fun setUp() {
        poseRepository.deleteAll()
        mediaRepository.deleteAll()
        val uploader = AdminTestSession.createUser(userRepository, RoleType.ADMIN.role)
        uploaderId = uploader.id!!
        uploaderCookie = AdminTestSession.login(sessions, uploader).cookie
        uploaderCsrf = AdminTestSession.csrf(mockMvc, uploaderCookie)
        val actingAdmin = AdminTestSession.createUser(userRepository, RoleType.ADMIN.role)
        adminCookie = AdminTestSession.login(sessions, actingAdmin).cookie
        csrf = AdminTestSession.csrf(mockMvc, adminCookie)
    }

    private fun seedMedia(type: DomainMediaType = DomainMediaType.POSE): Media = mediaRepository.save(
        Media(
            storageKey = "pose/test-${System.nanoTime()}.png",
            ownerId = uploaderId,
            mediaType = type,
            contentType = "image/png",
            adminManaged = true,
        ),
    )

    private fun adminPost(path: String) = post(path).cookie(adminCookie).header("X-CSRF-TOKEN", csrf)

    private fun adminPatch(path: String, vararg uriVariables: Any) =
        patch(path, *uriVariables).cookie(adminCookie).header("X-CSRF-TOKEN", csrf)

    private fun issueMediaAsUploader(): Media {
        val response = mockMvc.perform(
            post("/admin/v1/media/upload").cookie(uploaderCookie)
                .header("X-CSRF-TOKEN", uploaderCsrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"items":[{"mediaType":"POSE","contentType":"image/png"}]}"""),
        ).andExpect(status().isOk).andReturn().response
        val mediaId = objectMapper.readTree(response.contentAsString)["data"]["items"][0]["mediaId"].asLong()
        return mediaRepository.findById(mediaId).orElseThrow()
    }

    private fun seedPose(): Pose = poseRepository.save(Pose(userId = null, mediaId = 10L, headCount = HeadCount.ONE))

    @Test
    @DisplayName("일괄 등록 시 mediaId가 함께 저장된다")
    fun uploadPosesStoresMediaIds() {
        val first: Media = issueMediaAsUploader()
        val second: Media = seedMedia()
        mockMvc.perform(
            adminPost("/admin/v1/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"uploads":[
                        {"mediaId":${first.id},"headCount":"ONE","memo":null},
                        {"mediaId":${second.id},"headCount":"TWO","memo":"둘이서"}
                    ]}""",
                ),
        ).andExpect(status().isCreated)

        val saved: List<Pose> = poseRepository.findAll()
        saved.map { it.mediaId } shouldContainExactlyInAnyOrder listOf(first.id!!, second.id!!)
        saved.forEach { it.userId shouldBe null }
        mediaRepository.findById(first.id!!).get().ownerId shouldBe uploaderId
        mediaRepository.findById(first.id!!).get().status shouldBe MediaStatus.UPLOADED
        mediaRepository.findById(second.id!!).get().status shouldBe MediaStatus.UPLOADED
    }

    @Test
    @DisplayName("중복 mediaId가 섞이면 400이고 아무것도 저장되지 않는다")
    fun duplicateMediaIdsStoreNothing() {
        mockMvc.perform(
            adminPost("/admin/v1/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"uploads":[
                        {"mediaId":10,"headCount":"ONE","memo":null},
                        {"mediaId":10,"headCount":"TWO","memo":null}
                    ]}""",
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))

        poseRepository.count() shouldBe 0L
    }

    @Test
    fun missingMediaIdInUploadReturnsInvalidParameter() {
        mockMvc.perform(
            adminPost("/admin/v1/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"uploads":[{"mediaId":null,"headCount":"ONE","memo":null}]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))

        poseRepository.count() shouldBe 0L
    }

    @Test
    @DisplayName("이미지 교체 시 mediaId가 갱신된다")
    fun updatePoseMediaReplacesImage() {
        val pose: Pose = seedPose()
        val media: Media = seedMedia()

        mockMvc.perform(
            adminPatch("/admin/v1/pose/{poseId}", pose.id!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mediaId":${media.id}}"""),
        ).andExpect(status().isOk)

        poseRepository.findById(pose.id!!).get().mediaId shouldBe media.id
        mediaRepository.findById(media.id!!).get().status shouldBe MediaStatus.UPLOADED
    }

    @Test
    fun missingMediaDoesNotCreatePose() {
        mockMvc.perform(
            adminPost("/admin/v1/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"uploads":[{"mediaId":99999,"headCount":"ONE","memo":null}]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.UPLOAD_FAILED.code))

        poseRepository.count() shouldBe 0L
    }

    @Test
    @DisplayName("일반 사용자 업로드 미디어는 관리자가 포즈에 연결할 수 없다")
    fun userMediaCannotBecomeAdminPose() {
        val user = AdminTestSession.createUser(userRepository)
        val ordinary = mediaRepository.save(
            Media(
                storageKey = "pose/ordinary-${System.nanoTime()}.png",
                ownerId = user.id!!,
                mediaType = DomainMediaType.POSE,
                contentType = "image/png",
            ),
        )

        mockMvc.perform(
            adminPost("/admin/v1/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"uploads":[{"mediaId":${ordinary.id},"headCount":"ONE"}]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.UPLOAD_FAILED.code))
        poseRepository.count() shouldBe 0L
        mediaRepository.findById(ordinary.id!!).get().status shouldBe MediaStatus.INITIATED
    }

    @Test
    @DisplayName("mediaId 없이 교체를 요청하면 400이고 기존 이미지가 유지된다")
    fun updateWithoutMediaIdReturns400() {
        val pose: Pose = seedPose()

        mockMvc.perform(
            adminPatch("/admin/v1/pose/{poseId}", pose.id!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))

        poseRepository.findById(pose.id!!).get().mediaId shouldBe 10L
    }

    @Test
    @DisplayName("존재하지 않는 포즈를 교체하면 400 NOT_FOUND")
    fun updateMissingPoseReturnsNotFound() {
        mockMvc.perform(
            adminPatch("/admin/v1/pose/{poseId}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mediaId":20}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.NOT_FOUND.code))
    }
}
