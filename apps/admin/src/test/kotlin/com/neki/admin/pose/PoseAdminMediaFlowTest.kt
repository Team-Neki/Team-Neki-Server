package com.neki.admin.pose

import com.neki.admin.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.core.code.ResultCode
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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
    lateinit var poseRepository: JpaPoseRepository

    @BeforeEach
    fun setUp() {
        poseRepository.deleteAll()
    }

    private fun seedPose(): Pose = poseRepository.save(Pose(userId = null, mediaId = 10L, headCount = HeadCount.ONE))

    @Test
    @DisplayName("일괄 등록 시 mediaId가 함께 저장된다")
    fun uploadPosesStoresMediaIds() {
        mockMvc.perform(
            post("/admin/v1/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"uploads":[
                        {"mediaId":10,"headCount":"ONE","memo":null},
                        {"mediaId":11,"headCount":"TWO","memo":"둘이서"}
                    ]}""",
                ),
        ).andExpect(status().isCreated)

        val saved: List<Pose> = poseRepository.findAll()
        saved.map { it.mediaId } shouldContainExactlyInAnyOrder listOf(10L, 11L)
        saved.forEach { it.userId shouldBe null }
    }

    @Test
    @DisplayName("중복 mediaId가 섞이면 400이고 아무것도 저장되지 않는다")
    fun duplicateMediaIdsStoreNothing() {
        mockMvc.perform(
            post("/admin/v1/pose")
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
    @DisplayName("이미지 교체 시 mediaId가 갱신된다")
    fun updatePoseMediaReplacesImage() {
        val pose: Pose = seedPose()

        mockMvc.perform(
            patch("/admin/v1/pose/{poseId}", pose.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mediaId":20}"""),
        ).andExpect(status().isOk)

        poseRepository.findById(pose.id!!).get().mediaId shouldBe 20L
    }

    @Test
    @DisplayName("mediaId 없이 교체를 요청하면 400이고 기존 이미지가 유지된다")
    fun updateWithoutMediaIdReturns400() {
        val pose: Pose = seedPose()

        mockMvc.perform(
            patch("/admin/v1/pose/{poseId}", pose.id)
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
            patch("/admin/v1/pose/{poseId}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mediaId":20}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value(ResultCode.NOT_FOUND.code))
    }
}
