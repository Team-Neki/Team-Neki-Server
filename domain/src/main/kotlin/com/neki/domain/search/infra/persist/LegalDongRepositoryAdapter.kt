package com.neki.domain.search.infra.persist

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.infra.persist.jpa.LegalDongQueryRepository
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.repository.LegalDongRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : LegalDongRepositoryAdapter
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : LegalDong 영속성 Adapter
 */
@Repository
class LegalDongRepositoryAdapter(private val queryRepository: LegalDongQueryRepository) : LegalDongRepository {

    override fun findByNameOrPathPrefix(
        namePrefix: String,
        pathPrefix: String?,
        pagination: Pagination,
    ): List<LegalDong> = queryRepository.findByNameOrPathPrefix(namePrefix, pathPrefix, pagination)

    override fun countByNameOrPathPrefix(namePrefix: String, pathPrefix: String?): Long =
        queryRepository.countByNameOrPathPrefix(namePrefix, pathPrefix)
}
