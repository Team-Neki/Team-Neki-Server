package com.neki.domain.search.infra.persist

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.infra.persist.jpa.SubwayStationQueryRepository
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.UserLocation
import com.neki.domain.search.repository.SubwayStationRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : SubwayStationRepositoryAdapter
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : SubwayStation 영속성 Adapter
 */
@Repository
class SubwayStationRepositoryAdapter(private val queryRepository: SubwayStationQueryRepository) :
    SubwayStationRepository {

    override fun findByKeywordPrefix(
        keyword: String,
        namePrefix: String,
        userLocation: UserLocation?,
        pagination: Pagination,
    ): List<SubwayStation> = queryRepository.findByKeywordPrefix(keyword, namePrefix, userLocation, pagination)

    override fun countByKeywordPrefix(keyword: String, namePrefix: String): Long =
        queryRepository.countByKeywordPrefix(keyword, namePrefix)
}
