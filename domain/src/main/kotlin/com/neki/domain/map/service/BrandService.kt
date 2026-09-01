package com.neki.domain.map.service

import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.CountedPage
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.BrandCommand
import com.neki.domain.map.dto.BrandQuery
import com.neki.domain.map.models.Brand
import com.neki.domain.map.repository.BrandRepository
import org.springframework.stereotype.Component

/**
 * fileName       : BrandService
 * author         : koo
 * date           : 2026. 8. 3. 오전 12:26
 * description    : 브랜드 자체의 조회와 상태 변경. 사용자별 정렬은 [BrandOrderService] 가 맡는다
 */
@Component
class BrandService(private val brandRepository: BrandRepository) {

    fun getBrands(query: BrandQuery.GetBrands): CountedPage<Brand> = CountedPage(
        items = brandRepository.findAll(query),
        totalCount = brandRepository.count(query),
        size = query.pagination.size,
    )

    fun searchBrands(query: BrandQuery.SearchBrands): CountedPage<Brand> = CountedPage(
        items = brandRepository.findByKeyword(query),
        totalCount = brandRepository.countByKeyword(query),
        size = query.pagination.size,
    )

    fun updateBrand(command: BrandCommand.UpdateBrand): Brand {
        if (command.hasNoChanges) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }

        val brand: Brand = brandRepository.findById(command.brandId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        val newName: String? = command.name.takeIf { it != brand.name }
        val newCode: String? = command.code.takeIf { it != brand.code }
        validateNoDuplicate(newName, newCode)

        brand.updateInfo(
            command.name,
            command.code,
            command.supportAndroidQr,
            command.supportIosQr,
            command.exposeToMap,
        )
        return brand
    }

    fun deleteBrand(command: BrandCommand.DeleteBrand) {
        val brand: Brand = brandRepository.findById(command.brandId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)
        brand.softDelete()
    }

    fun addBrand(command: BrandCommand.AddBrand) {
        validateNoDuplicate(command.name, command.code)

        val brand = Brand.of(
            command.name,
            command.code,
            command.mediaId,
            command.supportAndroidQr,
            command.supportIosQr,
        )
        brandRepository.save(brand)
    }

    /**
     * name, code 는 유니크 제약이 있다. null 인 인자는 검사하지 않는다.
     */
    private fun validateNoDuplicate(name: String?, code: String?) {
        if (name != null && brandRepository.existsByName(name)) {
            throw BusinessException(ResultCode.CONFLICT_BRAND)
        }
        if (code != null && brandRepository.existsByCode(code)) {
            throw BusinessException(ResultCode.CONFLICT_BRAND)
        }
    }
}
