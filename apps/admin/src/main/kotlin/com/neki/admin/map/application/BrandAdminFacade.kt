package com.neki.admin.map.application

import com.neki.admin.map.api.BrandAdminDto
import com.neki.core.domain.vo.CountedPage
import com.neki.domain.map.dto.BrandCommand
import com.neki.domain.map.dto.BrandQuery
import com.neki.domain.map.models.Brand
import com.neki.domain.map.service.BrandService
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
class BrandAdminFacade(private val brandService: BrandService) {

    fun getBrands(query: BrandQuery.GetBrands): BrandAdminDto.Response.GetBrands {
        val brands: CountedPage<Brand> = brandService.getBrands(query)
        return BrandAdminDto.Response.GetBrands.of(brands)
    }

    fun searchBrands(query: BrandQuery.SearchBrands): BrandAdminDto.Response.GetBrands {
        val brands: CountedPage<Brand> = brandService.searchBrands(query)
        return BrandAdminDto.Response.GetBrands.of(brands)
    }

    @Transactional
    fun updateBrand(command: BrandCommand.UpdateBrand) = brandService.updateBrand(command)

    @Transactional
    fun deleteBrand(command: BrandCommand.DeleteBrand) = brandService.deleteBrand(command)

    @Transactional
    fun addBrand(command: BrandCommand.AddBrand) = brandService.addBrand(command)
}
