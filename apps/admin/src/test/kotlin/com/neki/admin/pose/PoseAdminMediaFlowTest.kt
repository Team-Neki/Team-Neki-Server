package com.neki.admin.pose

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.admin.auth.AdminTestSession
import com.neki.admin.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.core.code.ResultCode
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.infra.persist.jpa.JpaMediaRepository
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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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
    lateinit var mediaRepository: JpaMediaRepository

    @Autowired
    lateinit var userRepository: JpaUserRepository

    @Autowired
    lateinit var sessions: JdbcIndexedSessionRepository

    // 테스트용 저장소는 항상 객체가 있다고 답하므로 S3 객체가 없는 경우만 덮어쓴다
    @MockitoSpyBean
    lateinit var mediaStorage: MediaStorage

    private lateinit var adminCookie: Cookie
    private lateinit var uploaderCookie: Cookie
    private var uploaderId: Long = 0

    @BeforeEach
    fun setUp() {
        poseRepository.deleteAll()
        mediaRepository.deleteAll()
        val uploader = AdminTestSession.createUser(userRepository, RoleType.ADMIN.role)
        uploaderId = uploader.id!!
        uploaderCookie = AdminTestSession.login(sessions, uploader).cookie
        val actingAdmin = AdminTestSession.createUser(userRepository, RoleType.ADMIN.role)
        adminCookie = AdminTestSession.login(sessions, actingAdmin).cookie
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

    private fun adminPost(path: String) = post(path).cookie(adminCookie)

    private fun adminPatch(path: String, vararg uriVariables: Any) = patch(path, *uriVariables).cookie(adminCookie)

    private fun issueMediaAsUploader(): Media {
        val response = mockMvc.perform(
            post("/api/media/upload").cookie(uploaderCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"items":[{"mediaType":"POSE","contentType":"image/png"}]}"""),
        ).andExpect(status().isOk).andReturn().response
        val mediaId = objectMapper.readTree(response.contentAsString)["data"]["items"][0]["mediaId"].asLong()
        return mediaRepository.findById(mediaId).orElseThrow()
    }

    private fun seedPose(mediaId: Long = 10L, headCount: HeadCount = HeadCount.ONE): Pose =
        poseRepository.save(Pose(userId = null, mediaId = mediaId, headCount = headCount))

    private fun uploadsJson(vararg mediaIds: Long): String =
        mediaIds.joinToString(prefix = """{"uploads":[""", postfix = "]}") { """{"mediaId":$it,"headCount":"ONE"}""" }

    @Test
    @DisplayName("일괄 등록 시 mediaId가 함께 저장된다")
    fun uploadPosesStoresMediaIds() {
        val first: Media = issueMediaAsUploader()
        val second: Media = seedMedia()
        mockMvc.perform(
            adminPost("/api/pose")
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
            adminPost("/api/pose")
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
            adminPost("/api/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"uploads":[{"mediaId":null,"headCount":"ONE","memo":null}]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))

        poseRepository.count() shouldBe 0L
    }

    @Test
    fun emptyUploadsAndOversizedMemoReturn400() {
        mockMvc.perform(
            adminPost("/api/pose").contentType(MediaType.APPLICATION_JSON).content("""{"uploads":[]}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))

        mockMvc.perform(
            adminPost("/api/pose").contentType(MediaType.APPLICATION_JSON)
                .content("""{"uploads":[{"mediaId":1,"headCount":"ONE","memo":"${"a".repeat(256)}"}]}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))

        poseRepository.count() shouldBe 0L
    }

    @Test
    @DisplayName("이미지 교체 시 mediaId가 갱신된다")
    fun updatePoseMediaReplacesImage() {
        val pose: Pose = seedPose()
        val media: Media = seedMedia()

        mockMvc.perform(
            adminPatch("/api/pose/{poseId}", pose.id!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mediaId":${media.id}}"""),
        ).andExpect(status().isOk)

        poseRepository.findById(pose.id!!).get().mediaId shouldBe media.id
        mediaRepository.findById(media.id!!).get().status shouldBe MediaStatus.UPLOADED
    }

    @Test
    fun missingMediaDoesNotCreatePose() {
        mockMvc.perform(
            adminPost("/api/pose")
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
            adminPost("/api/pose")
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
            adminPatch("/api/pose/{poseId}", pose.id!!)
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
            adminPatch("/api/pose/{poseId}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mediaId":20}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("목록은 headCount 로 거르고 전체 건수·페이지 수를 함께 반환한다")
    fun getPosesFiltersByHeadCountAndCountsAll() {
        seedPose(mediaId = 1L)
        val latest: Pose = seedPose(mediaId = 2L)
        seedPose(mediaId = 3L, headCount = HeadCount.TWO)

        mockMvc.perform(get("/api/pose").cookie(adminCookie).param("headCount", "ONE").param("size", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.headCount").value("ONE"))
            .andExpect(jsonPath("$.data.totalCount").value(2))
            .andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.poses.length()").value(1))
            .andExpect(jsonPath("$.data.poses[0].id").value(latest.id!!))
    }

    @Test
    fun getPosesOverMaxSizeReturns400() {
        mockMvc.perform(get("/api/pose").cookie(adminCookie).param("size", "101"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("비로그인은 401, ADMIN 이 아닌 사용자는 403 이다")
    fun poseEndpointsRequireAdmin() {
        mockMvc.perform(get("/api/pose")).andExpect(status().isUnauthorized)

        val userCookie: Cookie = AdminTestSession.login(sessions, AdminTestSession.createUser(userRepository)).cookie
        mockMvc.perform(get("/api/pose").cookie(userCookie)).andExpect(status().isForbidden)
        mockMvc.perform(
            post("/api/pose").cookie(userCookie).contentType(MediaType.APPLICATION_JSON).content(uploadsJson(1L)),
        ).andExpect(status().isForbidden)
        poseRepository.count() shouldBe 0L
    }

    @Test
    @DisplayName("한 번에 10건을 넘겨 등록하면 400이다")
    fun uploadOverMaxBatchReturns400() {
        mockMvc.perform(
            adminPost("/api/pose").contentType(MediaType.APPLICATION_JSON).content(
                uploadsJson(
                    *LongArray(11) {
                        it + 1L
                    },
                ),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))
        poseRepository.count() shouldBe 0L
    }

    @Test
    @DisplayName("다른 포즈가 쓰는 media 로 등록하면 400이고 아무것도 저장되지 않는다")
    fun uploadWithMediaInUseStoresNothing() {
        val used: Media = seedMedia()
        seedPose(mediaId = used.id!!)
        val fresh: Media = seedMedia()

        mockMvc.perform(
            adminPost("/api/pose").contentType(MediaType.APPLICATION_JSON).content(uploadsJson(fresh.id!!, used.id!!)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))
        poseRepository.count() shouldBe 1L
        mediaRepository.findById(fresh.id!!).get().status shouldBe MediaStatus.INITIATED
    }

    @Test
    @DisplayName("다른 포즈가 쓰는 media 로 교체하면 400이고 기존 이미지가 유지된다")
    fun updateWithMediaInUseKeepsImage() {
        val pose: Pose = seedPose()
        seedPose(mediaId = 20L)

        mockMvc.perform(
            adminPatch(
                "/api/pose/{poseId}",
                pose.id!!,
            ).contentType(MediaType.APPLICATION_JSON).content("""{"mediaId":20}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))
        poseRepository.findById(pose.id!!).get().mediaId shouldBe 10L
    }

    @Test
    @DisplayName("교체할 media 의 S3 객체가 없으면 UPLOAD_FAILED 이고 기존 이미지가 유지된다")
    fun updateWithMissingStorageObjectRollsBack() {
        val pose: Pose = seedPose()
        val media: Media = seedMedia()
        doReturn(false).`when`(mediaStorage).exists(anyString())

        mockMvc.perform(
            adminPatch(
                "/api/pose/{poseId}",
                pose.id!!,
            ).contentType(MediaType.APPLICATION_JSON).content("""{"mediaId":${media.id}}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.UPLOAD_FAILED.code))
        poseRepository.findById(pose.id!!).get().mediaId shouldBe 10L
        mediaRepository.findById(media.id!!).get().status shouldBe MediaStatus.INITIATED
    }

    @Test
    @DisplayName("POSE 가 아닌 media 로 교체하면 UPLOAD_FAILED 이고 기존 이미지가 유지된다")
    fun updateWithNonPoseMediaRollsBack() {
        val pose: Pose = seedPose()
        val logo: Media = seedMedia(DomainMediaType.LOGO)

        mockMvc.perform(
            adminPatch(
                "/api/pose/{poseId}",
                pose.id!!,
            ).contentType(MediaType.APPLICATION_JSON).content("""{"mediaId":${logo.id}}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.UPLOAD_FAILED.code))
        poseRepository.findById(pose.id!!).get().mediaId shouldBe 10L
    }
}
