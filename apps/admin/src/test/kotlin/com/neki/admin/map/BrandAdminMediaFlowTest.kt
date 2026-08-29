package com.neki.admin.map

import com.neki.admin.map.infra.persist.jpa.JpaBrandRepository
import com.neki.core.code.ResultCode
import com.neki.domain.map.models.Brand
import io.kotest.matchers.booleans.shouldBeTrue
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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
    lateinit var brandRepository: JpaBrandRepository

    @BeforeEach
    fun setUp() {
        brandRepository.deleteAll()
    }

    private fun seedBrand(): Brand =
        brandRepository.save(Brand.of("포토그레이", "PHOTOGRAY", 1L, supportAndroidQr = true, supportIosQr = true))

    @Test
    @DisplayName("등록 시 mediaId가 함께 저장된다")
    fun addBrandStoresMediaId() {
        mockMvc.perform(
            post("/admin/v1/brand")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"name":"포토그레이","code":"PHOTOGRAY","mediaId":1,
                        "supportAndroidQr":true,"supportIosQr":true,"exposeToMap":false}""",
                ),
        ).andExpect(status().isCreated)

        val saved: Brand? = brandRepository.findByCode("PHOTOGRAY")
        saved!!.mediaId shouldBe 1L
    }

    @Test
    @DisplayName("mediaId만 넘겨 수정하면 이미지만 교체된다")
    fun updateOnlyMediaId() {
        val brand: Brand = seedBrand()

        mockMvc.perform(
            patch("/admin/v1/brand/{brandId}", brand.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mediaId":2}"""),
        ).andExpect(status().isOk)

        val updated: Brand = brandRepository.findById(brand.id!!).get()
        updated.mediaId shouldBe 2L
        updated.name shouldBe "포토그레이"
    }

    @Test
    @DisplayName("mediaId 없이 수정하면 기존 이미지가 유지된다")
    fun updateWithoutMediaIdKeepsImage() {
        val brand: Brand = seedBrand()

        mockMvc.perform(
            patch("/admin/v1/brand/{brandId}", brand.id)
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
            patch("/admin/v1/brand/{brandId}", brand.id)
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
            patch("/admin/v1/brand/{brandId}", 99999L)
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

        mockMvc.perform(delete("/admin/v1/brand/{brandId}", brand.id))
            .andExpect(status().isOk)

        val deleted: Brand = brandRepository.findById(brand.id!!).get()
        deleted.isDeleted.shouldBeTrue()
        deleted.mediaId shouldBe 1L
    }
}
