package com.neki.admin.map

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.admin.auth.AdminTestSession
import com.neki.admin.common.media.JpaAdminMediaRepository
import com.neki.admin.map.infra.persist.jpa.JpaBrandRepository
import com.neki.core.code.ResultCode
import com.neki.domain.map.models.Brand
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import com.neki.domain.user.models.RoleType
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import com.neki.domain.media.models.MediaType as DomainMediaType

/**
 * fileName       : BrandAdminMediaFlowTest
 * description    : 브랜드 이미지(mediaId) 등록·수정·삭제 flow와 예외 처리 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrandAdminMediaFlowTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var brandRepository: JpaBrandRepository

    @Autowired
    lateinit var mediaRepository: JpaAdminMediaRepository

    @Autowired
    lateinit var userRepository: JpaUserRepository

    @Autowired
    lateinit var sessions: JdbcIndexedSessionRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var adminCookie: Cookie
    private lateinit var csrf: String
    private lateinit var uploaderCookie: Cookie
    private lateinit var uploaderCsrf: String
    private var uploaderId: Long = 0

    @BeforeEach
    fun setUp() {
        brandRepository.deleteAll()
        mediaRepository.deleteAll()
        val uploader = AdminTestSession.createUser(userRepository, RoleType.ADMIN.role)
        uploaderId = uploader.id!!
        uploaderCookie = AdminTestSession.login(sessions, uploader).cookie
        uploaderCsrf = AdminTestSession.csrf(mockMvc, uploaderCookie)
        val actingAdmin = AdminTestSession.createUser(userRepository, RoleType.ADMIN.role)
        adminCookie = AdminTestSession.login(sessions, actingAdmin).cookie
        csrf = AdminTestSession.csrf(mockMvc, adminCookie)
    }

    private fun seedMedia(type: DomainMediaType = DomainMediaType.LOGO): Media = mediaRepository.save(
        Media(
            storageKey = "logo/test-${System.nanoTime()}.png",
            ownerId = uploaderId,
            mediaType = type,
            contentType = "image/png",
            adminManaged = true,
        ),
    )

    private fun adminPost(path: String) = post(path).cookie(adminCookie).header("X-CSRF-TOKEN", csrf)

    private fun adminPatch(path: String, vararg uriVariables: Any) =
        patch(path, *uriVariables).cookie(adminCookie).header("X-CSRF-TOKEN", csrf)

    private fun adminDelete(path: String, vararg uriVariables: Any) =
        delete(path, *uriVariables).cookie(adminCookie).header("X-CSRF-TOKEN", csrf)

    private fun issueMediaAsUploader(): Media {
        val response = mockMvc.perform(
            post("/admin/v1/media/upload").cookie(uploaderCookie)
                .header("X-CSRF-TOKEN", uploaderCsrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"items":[{"mediaType":"LOGO","contentType":"image/png"}]}"""),
        ).andExpect(status().isOk).andReturn().response
        val mediaId = objectMapper.readTree(response.contentAsString)["data"]["items"][0]["mediaId"].asLong()
        return mediaRepository.findById(mediaId).orElseThrow()
    }

    private fun seedBrand(): Brand =
        brandRepository.save(Brand.of("포토그레이", "PHOTOGRAY", 1L, supportAndroidQr = true, supportIosQr = true))

    @Test
    @DisplayName("등록 시 mediaId와 exposeToMap이 함께 저장된다")
    fun addBrandStoresMediaId() {
        val media: Media = issueMediaAsUploader()
        mockMvc.perform(
            adminPost("/admin/v1/brand")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"name":"포토그레이","code":"PHOTOGRAY","mediaId":${media.id},
                        "supportAndroidQr":true,"supportIosQr":true,"exposeToMap":true}""",
                ),
        ).andExpect(status().isCreated)

        val saved: Brand? = brandRepository.findByCode("PHOTOGRAY")
        saved!!.mediaId shouldBe media.id
        saved.exposeToMap shouldBe true
        mediaRepository.findById(media.id!!).get().ownerId shouldBe uploaderId
        mediaRepository.findById(media.id!!).get().status shouldBe MediaStatus.UPLOADED

        mockMvc.perform(get("/admin/v1/brand").cookie(adminCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.brandInfos[0].id").value(saved.id))
    }

    @Test
    @DisplayName("mediaId만 넘겨 수정하면 이미지만 교체된다")
    fun updateOnlyMediaId() {
        val brand: Brand = seedBrand()
        val media: Media = seedMedia()

        mockMvc.perform(
            adminPatch("/admin/v1/brand/{brandId}", brand.id!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mediaId":${media.id}}"""),
        ).andExpect(status().isOk)

        val updated: Brand = brandRepository.findById(brand.id!!).get()
        updated.mediaId shouldBe media.id
        updated.name shouldBe "포토그레이"
        mediaRepository.findById(media.id!!).get().status shouldBe MediaStatus.UPLOADED
    }

    @Test
    fun wrongMediaTypeDoesNotCreateBrand() {
        val media: Media = seedMedia(DomainMediaType.POSE)

        mockMvc.perform(
            adminPost("/admin/v1/brand")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"name":"포토그레이","code":"PHOTOGRAY","mediaId":${media.id},
                    "supportAndroidQr":true,"supportIosQr":true,"exposeToMap":true}""",
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.UPLOAD_FAILED.code))

        brandRepository.count() shouldBe 0L
    }

    @Test
    @DisplayName("일반 사용자 업로드 미디어는 관리자가 브랜드에 연결할 수 없다")
    fun userMediaCannotBecomeAdminBrand() {
        val user = AdminTestSession.createUser(userRepository)
        val ordinary = mediaRepository.save(
            Media(
                storageKey = "logo/ordinary-${System.nanoTime()}.png",
                ownerId = user.id!!,
                mediaType = DomainMediaType.LOGO,
                contentType = "image/png",
            ),
        )

        mockMvc.perform(
            adminPost("/admin/v1/brand")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"name":"포토그레이","code":"PHOTOGRAY","mediaId":${ordinary.id},
                        "supportAndroidQr":true,"supportIosQr":true,"exposeToMap":true}""",
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.UPLOAD_FAILED.code))
        brandRepository.count() shouldBe 0L
        mediaRepository.findById(ordinary.id!!).get().status shouldBe MediaStatus.INITIATED
    }

    @Test
    @DisplayName("mediaId 없이 수정하면 기존 이미지가 유지된다")
    fun updateWithoutMediaIdKeepsImage() {
        val brand: Brand = seedBrand()

        mockMvc.perform(
            adminPatch("/admin/v1/brand/{brandId}", brand.id!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"포토이즘"}"""),
        ).andExpect(status().isOk)

        val updated: Brand = brandRepository.findById(brand.id!!).get()
        updated.mediaId shouldBe 1L
        updated.name shouldBe "포토이즘"
    }

    @Test
    @DisplayName("변경할 필드가 하나도 없으면 400 INVALID_PARAMETER")
    fun updateWithNoChangesReturns400() {
        val brand: Brand = seedBrand()

        mockMvc.perform(
            adminPatch("/admin/v1/brand/{brandId}", brand.id!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("존재하지 않는 브랜드를 수정하면 400 NOT_FOUND")
    fun updateMissingBrandReturnsNotFound() {
        mockMvc.perform(
            adminPatch("/admin/v1/brand/{brandId}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mediaId":2}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("삭제는 soft delete이고 media 연결은 그대로 남는다")
    fun deleteBrandIsSoftDelete() {
        val brand: Brand = seedBrand()

        mockMvc.perform(adminDelete("/admin/v1/brand/{brandId}", brand.id!!))
            .andExpect(status().isOk)

        // soft delete된 행은 @SQLRestriction 때문에 JPA로는 보이지 않으므로 native SQL로 확인한다
        val row: Map<String, Any?> =
            jdbcTemplate.queryForMap("SELECT deleted_at, media_id FROM TB_BRAND WHERE id = ?", brand.id)
        row["deleted_at"].shouldNotBeNull()
        row["media_id"] shouldBe 1L
    }
}
