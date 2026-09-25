package com.neki.batch.search

import com.neki.domain.map.repository.BrandRepository
import com.neki.domain.search.client.BrandClient
import com.neki.domain.search.models.SearchBrand
import org.springframework.stereotype.Component

/**
 * fileName       : BrandClientAdapter
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : search 도메인의 BrandClient 를 map 도메인의 BrandRepository 로 잇는다 (apps/api 의 MapMediaClient 와 같은 패턴)
 */
@Component
class BrandClientAdapter(private val brandRepository: BrandRepository) : BrandClient {

    override fun findAll(): List<SearchBrand> = brandRepository.findAll().map {
        SearchBrand(id = it.id!!, name = it.name, code = it.code, platform = it.platform)
    }
}
