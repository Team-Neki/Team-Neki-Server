package com.neki.domain.search.infra.persist

import com.neki.domain.search.infra.persist.jpa.JpaPhotoBoothEnrichedRepository
import com.neki.domain.search.infra.persist.jpa.JpaPhotoBoothSearchRepository
import com.neki.domain.search.infra.persist.jpa.JpaSubwayStationRepository
import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.repository.PhotoBoothSearchRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoBoothSearchRepositoryAdapter
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : PhotoBoothSearchRepository 의 JPA 어댑터
 */
@Repository
class PhotoBoothSearchRepositoryAdapter(
    private val searchRepository: JpaPhotoBoothSearchRepository,
    private val enrichedRepository: JpaPhotoBoothEnrichedRepository,
    private val stationRepository: JpaSubwayStationRepository,
) : PhotoBoothSearchRepository {

    override fun findAllEnriched(): List<PhotoBoothEnriched> = enrichedRepository.findAll()

    override fun findAllStations(): List<SubwayStation> = stationRepository.findAll()

    override fun count(): Long = searchRepository.count()

    // 트랜잭션은 호출자(SearchIndexService)가 연다. 여기서 열면 DELETE 와 INSERT 가 한 트랜잭션이라는 보장이 흐려진다
    override fun replaceAll(cards: List<PhotoBoothSearch>) {
        searchRepository.deleteAllStations()
        searchRepository.deleteAllInBatch()
        searchRepository.saveAll(cards)
    }
}
