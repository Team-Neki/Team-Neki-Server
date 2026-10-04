package com.neki.domain.search.service.qu

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
 *   순서는 Normalize -> NER(메모리 사전) -> (범위를 못 찾았으면 DB 의 자동완성 keyword 정확 일치) -> Intent 생성이고, 순서와 사전은 이 서비스가 갖는다.
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
     * 메모리 사전으로 먼저 이해한다. 사전에 자동완성 keyword(지역 전체 경로, `역명역 노선명`, 부스 `브랜드명 지점명`)도 있어
     * longest match 로 그 하나가 되고, 서울 밖 지역(`경상남도 진주시 강남동`)도 전체 경로가 `강남` 보다 길어 이긴다.
     * 범위(지역·역·지점)를 하나도 못 찾았을 때만 DB 에서 자동완성 keyword 를 저장된 값 그대로 비교한다.
     * 사전이 아직 모르는 keyword(갱신 주기 사이에 색인에 들어온 지점, 사전 적재 실패)를 위한 것이다.
     */
    fun understand(keyword: String): QueryIntent {
        val collapsed: String = SearchNormalizer.collapseSpaces(keyword)
        val intent: QueryIntent = understand(collapsed, entityDictionaryCache.get())
        if (intent.targets.any { it is SearchTarget.Scope }) return intent

        val completions: List<SearchTarget> = findCompletions(collapsed)
        if (completions.isEmpty()) return intent

        return QueryIntent.of(
            intent.keyword,
            completions.map { ResolvedEntity(intent.keyword, 0, intent.keyword.length, it) },
        )
    }

    private fun findCompletions(collapsed: String): List<SearchTarget> = listOfNotNull(
        legalDongRepository.findByFullName(collapsed)?.let { SearchTarget.Region(code = it.code) },
        SubwayStation.parseKeyword(collapsed)?.let { subwayStationRepository.findById(it) }?.let {
            SearchTarget.Station(name = it.name, lineName = it.lineName)
        },
    ) + photoBoothSearchRepository.findByBoothName(collapsed).map { SearchTarget.Booth(it.platform, it.idx) }

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
            regions = legalDongRepository.findAllBelowSido(),
            stations = subwayStationRepository.findAll(),
            brandNames = photoBoothSearchRepository.findIndexedBrandNames(),
            booths = photoBoothSearchRepository.findAllCurrent(),
        )
        entityDictionaryCache.replace(dictionary)

        return dictionary.size
    }
}
