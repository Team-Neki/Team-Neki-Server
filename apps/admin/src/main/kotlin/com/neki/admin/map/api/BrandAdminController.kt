package com.neki.admin.map.api

import com.neki.admin.map.application.BrandAdminFacade
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.map.dto.BrandCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : BrandAdminController
 * author         : koo
 * date           : 2026. 8. 8. 오후 5:53
 * description    :
 */
@RestController
@RequestMapping("/admin/v1/brand")
class BrandAdminController(private val brandAdminFacade: BrandAdminFacade) {

    @GetMapping
    fun getBrands(@Valid request: BrandAdminDto.Request.GetBrands): BaseResponse<BrandAdminDto.Response.GetBrands> =
        BaseResponse(data = brandAdminFacade.getBrands(request.toQuery()))

    @GetMapping("/search")
    fun searchBrands(
        @Valid request: BrandAdminDto.Request.SearchBrands,
    ): BaseResponse<BrandAdminDto.Response.GetBrands> =
        BaseResponse(data = brandAdminFacade.searchBrands(request.toQuery()))

    @PatchMapping("/{brandId}")
    fun updateBrand(
        @RequestBody @Valid request: BrandAdminDto.Request.UpdateBrand,
        @PathVariable brandId: Long,
    ): BaseResponse<Any> {
        brandAdminFacade.updateBrand(toUpdateCommand(brandId, request))
        return BaseResponse(data = null)
    }

    @DeleteMapping("/{brandId}")
    fun deleteBrand(@PathVariable brandId: Long): BaseResponse<Any> {
        brandAdminFacade.deleteBrand(BrandCommand.DeleteBrand(brandId))
        return BaseResponse(data = null)
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun addBrand(@RequestBody @Valid request: BrandAdminDto.Request.AddBrand): BaseResponse<Any> {
        brandAdminFacade.addBrand(request.toCommand())
        return BaseResponse(data = null)
    }
}
