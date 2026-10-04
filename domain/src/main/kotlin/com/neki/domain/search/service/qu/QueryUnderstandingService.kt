package com.neki.domain.search.service.qu

import com.neki.domain.search.SearchNormalizer
import com.neki.domain.search.external.EntityDictionaryCache
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.qu.EntityDictionary
import com.neki.domain.search.models.qu.QueryIntent
import com.neki.domain.search.models.qu.ResolvedEntity
import com.neki.domain.search.repository.LegalDongRepository
import com.neki.domain.search.repository.PhotoBoothSearchRepository
import com.neki.domain.search.repository.SubwayStationRepository
import org.springframework.stereotype.Component

/**
 * fileName       : QueryUnderstandingService
 * author         : koo
 * date           : 2026. 9. 17.
 * description    : QU. 검색어를 받아 어떻게 이해했는지(QueryIntent)를 만드는 과정 전체이며, 검색어 해석의 유일한 입구다.
 *   순서는 Normalize -> (자동완성 keyword 면 그 지역·역·지점 | 아니면 NER) -> Intent 생성이고, 순서와 사전은 이 서비스가 갖는다.
 *   정규화 규칙 자체는 검색 색인(batch)과 사전 키가 같이 쓰므로 SearchNormalizer 한 곳에 있다.
 *   e.g. "강남" 은 지역, "강남역" 은 역, "강남점" 은 지점 (NER 이 지점명 속 지명을 엔티티로 잡지 않는다)
 */
@Component
class QueryUnderstandingService(
    private val legalDongRepository: LegalDongRepository,
    private val subwayStationRepository: SubwayStationRepository,
    private val photoBoothSearchRepository: PhotoBoothSearchRepository,
    private val entityDictionaryCache: EntityDictionaryCache,
) {

    /**
     * 자동완성 keyword(지역 전체 경로, `역명역 노선명`, 부스 `브랜드명 지점명`)를 먼저 본다.
     * 서울 밖 지역(`경상남도 진주시 강남동`)이 사전의 `강남` 으로 잘못 잡히지 않게 하고, 지점을 고르면 그 지점 하나만 나오게 하려는 것이다.
     * 자동완성 keyword 는 저장된 값과 그대로 비교하고, Intent 는 NER 경로와 같이 정규화한 검색어로 만든다.
     */
    fun understand(keyword: String): QueryIntent {
        val collapsed: String = SearchNormalizer.collapseSpaces(keyword)

        val completions: List<SearchTarget> = listOfNotNull(
            legalDongRepository.findByFullName(collapsed)?.let { SearchTarget.Region(code = it.code) },
            SubwayStation.parseKeyword(collapsed)?.let { subwayStationRepository.findById(it) }?.let {
                SearchTarget.Station(name = it.name, lineName = it.lineName)
            },
        ) + photoBoothSearchRepository.findByBoothName(collapsed).map { SearchTarget.Booth(it.platform, it.idx) }
        if (completions.isEmpty()) return understand(collapsed, entityDictionaryCache.get())

        val normalized: String = SearchNormalizer.normalize(collapsed)

        return QueryIntent.of(normalized, completions.map { ResolvedEntity(normalized, 0, normalized.length, it) })
    }

    /**
     * 자동완성 검색어에 적힌 브랜드. `강남 포토이즘` -> 포토이즘. 자동완성이 내려주는 filterGroup 의 브랜드 필터로 쓴다.
     * 브랜드는 NER 사전으로만 찾으므로 브랜드 이름 전체를 적어야 한다 (`포토이` 는 브랜드가 아니다). 검색어에 나온 순서다.
     */
    fun recognizeBrandIds(keyword: String): List<Long> = understand(keyword, entityDictionaryCache.get()).targets
        .filterIsInstance<SearchTarget.Brand>()
        .map { it.brandId }

    /** 사전에 올라간 브랜드 이름. 요청마다 DB 를 조회하지 않고 메모리의 사전에서 꺼낸다 */
    fun brandNames(): List<String> = entityDictionaryCache.get().brandNames

    internal fun understand(keyword: String, dictionary: EntityDictionary): QueryIntent {
        val normalized: String = SearchNormalizer.normalize(keyword)
        val entities: List<ResolvedEntity> = Ner.recognize(normalized, dictionary)

        return QueryIntent.of(normalized, entities)
    }

    /**
     * 사전을 저장소에서 다시 만들어 통째로 바꿔 끼운다. 다 만든 뒤에 한 번에 바꾸므로 실패하면 이전 사전이 남는다.
     * 앱이 뜰 때와 10분마다 apps/api(SearchDictionaryRefresher)가 부른다. 올린 항목 수를 돌려준다.
     */
    fun reloadDictionary(): Int {
        val dictionary: EntityDictionary = EntityDictionary.of(
            districts = legalDongRepository.findSeoulDistricts(),
            stations = photoBoothSearchRepository.findAllStations(),
            brandNames = photoBoothSearchRepository.findIndexedBrandNames(),
        )
        entityDictionaryCache.replace(dictionary)

        return dictionary.size
    }
}
