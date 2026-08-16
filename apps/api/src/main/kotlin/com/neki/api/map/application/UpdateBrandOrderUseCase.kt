package com.neki.api.map.application

import com.neki.core.annotation.UseCase
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.service.BrandOrderService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateBrandOrderUseCase
 * author         : darren
 * date           : 2026. 6. 22.
 * description    : 사용자별 브랜드 정렬 순서 저장/갱신
 */
@UseCase
class UpdateBrandOrderUseCase(private val brandOrderService: BrandOrderService) {

    @Transactional
    fun execute(command: MapCommand.UpdateBrandOrder) {
        brandOrderService.updateBrandOrder(command)
    }
}
