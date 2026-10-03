package com.neki.api.map.application

import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.map.repository.BrandRepository

/**
 * fileName       : GetBrandNamesUseCase
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 삭제되지 않은 브랜드 이름 목록. 통합 검색이 검색어에서 브랜드 낱말을 가려내는 데 SearchMapClient 로 호출한다.
 */
@UseCase
class GetBrandNamesUseCase(
    private val brandRepository: BrandRepository,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(): List<String> = transactionRunner.readOnly { brandRepository.findAll().map { it.name } }
}
