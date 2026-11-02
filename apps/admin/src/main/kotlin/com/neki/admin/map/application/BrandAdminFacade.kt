package com.neki.admin.map.application

import com.neki.admin.common.media.AdminMediaVerifier
import com.neki.admin.map.api.BrandAdminDto
import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.CountedPage
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.BrandCommand
import com.neki.domain.map.dto.BrandQuery
import com.neki.domain.map.models.Brand
import com.neki.domain.map.repository.BrandReferenceRepository
import com.neki.domain.map.service.BrandService
import com.neki.domain.media.models.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : BrandAdminFacade
 * author         : koo
 * date           : 2026. 8. 8. 오후 5:56
 * description    :
 */
@Service
@Transactional(readOnly = true)
class BrandAdminFacade(
    private val brandService: BrandService,
    private val mediaVerifier: AdminMediaVerifier,
    private val brandReferenceRepository: BrandReferenceRepository,
) {

    fun getBrands(query: BrandQuery.GetBrands): BrandAdminDto.Response.GetBrands {
        val brands: CountedPage<Brand> = brandService.getBrands(query)
        return BrandAdminDto.Response.GetBrands.of(brands)
    }

    fun searchBrands(query: BrandQuery.SearchBrands): BrandAdminDto.Response.GetBrands {
        val brands: CountedPage<Brand> = brandService.searchBrands(query)
        return BrandAdminDto.Response.GetBrands.of(brands)
    }

    @Transactional
    fun updateBrand(command: BrandCommand.UpdateBrand) {
        // 기존 로고는 관리자 업로드 표시(admin_managed)가 없으므로, 로고를 바꿀 때만 새 미디어를 검증한다
        val previousMediaId: Long? = brandService.getBrand(command.brandId).mediaId
        brandService.updateBrand(command)
        command.mediaId?.takeIf { it != previousMediaId }?.let {
            mediaVerifier.verifyAndConfirm(listOf(it), MediaType.LOGO)
        }
    }

    @Transactional
    fun deleteBrand(command: BrandCommand.DeleteBrand) {
        val brand: Brand = brandService.getBrand(command.brandId)
        if (brandReferenceRepository.isReferenced(brand)) {
            throw BusinessException(ResultCode.BRAND_IN_USE)
        }
        brandService.deleteBrand(command)
    }

    @Transactional
    fun addBrand(command: BrandCommand.AddBrand) {
        brandService.addBrand(command)
        mediaVerifier.verifyAndConfirm(listOf(command.mediaId), MediaType.LOGO)
    }
}
