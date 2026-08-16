package com.neki.admin.map.api

import com.neki.core.domain.vo.Pagination
import com.neki.domain.map.dto.BrandCommand
import com.neki.domain.map.dto.BrandQuery

/**
 * fileName       : BrandAdminMapper
 * author         : koo
 * date           : 2026. 8. 8. 오후 6:31
 * description    :
 */
fun BrandAdminDto.Request.GetBrands.toQuery(): BrandQuery.GetBrands =
    BrandQuery.GetBrands(supportsQr, exposeToMap, Pagination(page, size))

fun BrandAdminDto.Request.SearchBrands.toQuery(): BrandQuery.SearchBrands =
    BrandQuery.SearchBrands(keyword, Pagination(page, size))

fun toUpdateCommand(brandId: Long, request: BrandAdminDto.Request.UpdateBrand): BrandCommand.UpdateBrand =
    BrandCommand.UpdateBrand(
        brandId,
        request.name,
        request.code,
        request.supportAndroidQr,
        request.supportIosQr,
        request.exposeToMap,
    )

fun BrandAdminDto.Request.AddBrand.toCommand(): BrandCommand.AddBrand =
    BrandCommand.AddBrand(name, code, mediaId!!, supportAndroidQr, supportIosQr)
