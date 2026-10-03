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

    override fun findByNamePrefix(prefix: String, pagination: Pagination): List<LegalDong> =
        queryRepository.findByNamePrefix(prefix, pagination)

    override fun countByNamePrefix(prefix: String): Long = queryRepository.countByNamePrefix(prefix)

    override fun findByFullName(fullName: String): LegalDong? = queryRepository.findByFullName(fullName)

    override fun findSeoulDistricts(): List<LegalDong> = queryRepository.findSeoulDistricts()
}
